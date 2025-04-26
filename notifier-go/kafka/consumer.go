package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"sync"
	"time"
	
	"github.com/event-processing/notifier-go/config"
	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/segmentio/kafka-go"
)

// Consumer handles consuming webhook events from Kafka
type Consumer struct {
	reader          *kafka.Reader
	config          *config.Config
	eventProcessing WebhookEventProcessing
	rateLimiter     RateLimiterService
	eventService    WebhookEventService
	metrics         MetricsService
	workerPool      *WorkerPool
}

// WebhookEventProcessing defines the interface for webhook event processing
type WebhookEventProcessing interface {
	Process(ctx context.Context, eventID string, eventPayload *dto.WebhookEventDTO, url string, payload dto.BaseEventDTO) error
}

// RateLimiterService defines the interface for rate limiting
type RateLimiterService interface {
	IsAllowed(ctx context.Context, accountID string) bool
	AreEventsAllowed(ctx context.Context, accountID string, count int) bool
}

// WebhookEventService defines the interface for webhook event service
type WebhookEventService interface {
	GetWebhookUrls(ctx context.Context, eventType string, eventIDs []string) (map[string]string, error)
	GetPayloads(ctx context.Context, eventType string, eventIDs []string) (map[string]dto.BaseEventDTO, error)
}

// MetricsService defines the interface for metrics collection
type MetricsService interface {
	IncrementKafkaEventCount(count int)
	RecordBatchProcessingTime(duration time.Duration)
}

// WorkerPool represents a pool of workers for processing events
type WorkerPool struct {
	workers chan struct{}
}

// NewWorkerPool creates a new worker pool
func NewWorkerPool(size int) *WorkerPool {
	return &WorkerPool{
		workers: make(chan struct{}, size),
	}
}

// Submit submits a task to the worker pool
func (p *WorkerPool) Submit(task func()) {
	p.workers <- struct{}{}
	go func() {
		defer func() { <-p.workers }()
		task()
	}()
}

// NewConsumer creates a new Kafka consumer
func NewConsumer(cfg *config.Config) (*Consumer, error) {
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:        []string{cfg.Kafka.BootstrapServers},
		Topic:          cfg.Kafka.Topics.WebhookEvent.Name,
		GroupID:        cfg.Kafka.Consumer.GroupID,
		MinBytes:       10e3, // 10KB
		MaxBytes:       10e6, // 10MB
		MaxWait:        cfg.Kafka.Consumer.PollTimeout,
		StartOffset:    kafka.FirstOffset,
		CommitInterval: time.Second,
	})
	
	return &Consumer{
		reader: reader,
		config: cfg,
		workerPool: NewWorkerPool(cfg.ThreadPool.KafkaConsumer.MaxSize),
	}, nil
}

// SetEventProcessing sets the event processing service
func (c *Consumer) SetEventProcessing(eventProcessing WebhookEventProcessing) {
	c.eventProcessing = eventProcessing
}

// SetRateLimiter sets the rate limiter service
func (c *Consumer) SetRateLimiter(rateLimiter RateLimiterService) {
	c.rateLimiter = rateLimiter
}

// SetEventService sets the event service
func (c *Consumer) SetEventService(eventService WebhookEventService) {
	c.eventService = eventService
}

// SetMetrics sets the metrics service
func (c *Consumer) SetMetrics(metrics MetricsService) {
	c.metrics = metrics
}

// Start starts consuming messages from Kafka
func (c *Consumer) Start(ctx context.Context) error {
	if c.eventProcessing == nil {
		return fmt.Errorf("event processing service not set")
	}
	if c.rateLimiter == nil {
		return fmt.Errorf("rate limiter service not set")
	}
	if c.eventService == nil {
		return fmt.Errorf("event service not set")
	}
	if c.metrics == nil {
		return fmt.Errorf("metrics service not set")
	}
	
	log.Printf("Starting Kafka consumer for topic: %s", c.config.Kafka.Topics.WebhookEvent.Name)
	
	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		default:
			// Read a batch of messages
			messages, err := c.readBatch(ctx)
			if err != nil {
				log.Printf("Error reading batch: %v", err)
				continue
			}
			
			if len(messages) == 0 {
				continue
			}
			
			// Process the batch
			c.processBatch(ctx, messages)
		}
	}
}

// readBatch reads a batch of messages from Kafka
func (c *Consumer) readBatch(ctx context.Context) ([]kafka.Message, error) {
	var messages []kafka.Message
	
	// Read up to MaxPollRecords messages
	for i := 0; i < c.config.Kafka.Consumer.MaxPollRecords; i++ {
		message, err := c.reader.FetchMessage(ctx)
		if err != nil {
			if len(messages) > 0 {
				// Return the messages we've read so far
				return messages, nil
			}
			return nil, err
		}
		
		messages = append(messages, message)
	}
	
	return messages, nil
}

// processBatch processes a batch of messages
func (c *Consumer) processBatch(ctx context.Context, messages []kafka.Message) {
	start := time.Now()
	defer func() {
		c.metrics.RecordBatchProcessingTime(time.Since(start))
	}()
	
	c.metrics.IncrementKafkaEventCount(len(messages))
	log.Printf("Received %d events", len(messages))
	
	// Group events by type
	eventsByType := make(map[string][]kafka.Message)
	for _, message := range messages {
		var event dto.WebhookEventDTO
		if err := json.Unmarshal(message.Value, &event); err != nil {
			log.Printf("Failed to unmarshal event: %v", err)
			continue
		}
		
		eventsByType[event.EventType] = append(eventsByType[event.EventType], message)
	}
	
	// Process each event type in parallel
	var wg sync.WaitGroup
	for eventType, typeMessages := range eventsByType {
		wg.Add(1)
		eventType := eventType
		typeMessages := typeMessages
		
		c.workerPool.Submit(func() {
			defer wg.Done()
			c.processEventGroup(ctx, eventType, typeMessages)
		})
	}
	
	wg.Wait()
	
	// Commit the messages
	if err := c.reader.CommitMessages(ctx, messages...); err != nil {
		log.Printf("Failed to commit messages: %v", err)
	}
}

// processEventGroup processes a group of events of the same type
func (c *Consumer) processEventGroup(ctx context.Context, eventType string, messages []kafka.Message) {
	log.Printf("Processing %d events of type %s", len(messages), eventType)
	
	// Filter events by rate limit
	var allowedEvents []kafka.Message
	for _, message := range messages {
		var event dto.WebhookEventDTO
		if err := json.Unmarshal(message.Value, &event); err != nil {
			log.Printf("Failed to unmarshal event: %v", err)
			continue
		}
		
		if c.rateLimiter.IsAllowed(ctx, event.AccountID) {
			allowedEvents = append(allowedEvents, message)
		} else {
			log.Printf("Rate limit exceeded for account %s, skipping event %s", event.AccountID, event.EventID)
		}
	}
	
	if len(allowedEvents) == 0 {
		log.Printf("No events allowed for processing after rate limiting for type %s", eventType)
		return
	}
	
	// Extract event IDs
	var eventIDs []string
	for _, message := range allowedEvents {
		var event dto.WebhookEventDTO
		if err := json.Unmarshal(message.Value, &event); err != nil {
			continue
		}
		eventIDs = append(eventIDs, event.EventID)
	}
	
	// Get webhook URLs and payloads
	webhookURLMap, err := c.eventService.GetWebhookUrls(ctx, eventType, eventIDs)
	if err != nil {
		log.Printf("Failed to fetch webhook URLs for event type %s: %v", eventType, err)
		return
	}
	
	payloadMap, err := c.eventService.GetPayloads(ctx, eventType, eventIDs)
	if err != nil {
		log.Printf("Failed to fetch payloads for event type %s: %v", eventType, err)
		return
	}
	
	// Process each event
	var wg sync.WaitGroup
	for _, message := range allowedEvents {
		wg.Add(1)
		message := message
		
		c.workerPool.Submit(func() {
			defer wg.Done()
			
			var event dto.WebhookEventDTO
			if err := json.Unmarshal(message.Value, &event); err != nil {
				log.Printf("Failed to unmarshal event: %v", err)
				return
			}
			
			eventID := event.EventID
			url := webhookURLMap[eventID]
			payload := payloadMap[eventID]
			
			if url == "" || payload == nil {
				log.Printf("Skipping event %s due to missing webhook data (URL: %s, Payload: %v)", eventID, url, payload)
				return
			}
			
			if err := c.eventProcessing.Process(ctx, eventID, &event, url, payload); err != nil {
				log.Printf("Failed to process event %s: %v", eventID, err)
			} else {
				log.Printf("Successfully processed event %s", eventID)
			}
		})
	}
	
	wg.Wait()
}

// Close closes the Kafka consumer
func (c *Consumer) Close() error {
	return c.reader.Close()
}
