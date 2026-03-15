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

// EventProducer defines the interface for publishing webhook events
type EventProducer interface {
	Publish(ctx context.Context, topic, key string, payload *dto.WebhookEventDTO) error
}

// Consumer handles consuming webhook events from Kafka
type Consumer struct {
	reader          *kafka.Reader
	config          *config.Config
	eventProcessing WebhookEventProcessing
	rateLimiter     RateLimiterService
	eventService    WebhookEventService
	eventProducer   EventProducer
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
		Brokers:            []string{cfg.Kafka.BootstrapServers},
		Topic:              cfg.Kafka.Topics.WebhookEvent.Name,
		GroupID:            cfg.Kafka.Consumer.GroupID,
		MinBytes:           1,    // Don't wait for data accumulation
		MaxBytes:           10e6, // 10MB
		MaxWait:            cfg.Kafka.Consumer.PollTimeout * time.Millisecond,
		StartOffset:        kafka.FirstOffset,
		CommitInterval:     time.Second,
		HeartbeatInterval:      3 * time.Second,
		SessionTimeout:        10 * time.Second,
		RebalanceTimeout:      5 * time.Second,
		JoinGroupBackoff:      time.Second,
		PartitionWatchInterval: time.Second,
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

// SetEventProducer sets the event producer for republishing rate-limited events
func (c *Consumer) SetEventProducer(producer EventProducer) {
	c.eventProducer = producer
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

// readBatch reads a batch of messages from Kafka.
// It blocks until the first message arrives, then collects additional messages
// within a short window before returning the batch.
func (c *Consumer) readBatch(ctx context.Context) ([]kafka.Message, error) {
	var messages []kafka.Message

	// Block until the first message arrives
	message, err := c.reader.FetchMessage(ctx)
	if err != nil {
		return nil, err
	}
	messages = append(messages, message)

	// Try to collect more messages within a short window (non-blocking drain)
	batchTimeout := c.config.Kafka.Consumer.PollTimeout * time.Millisecond
	if batchTimeout <= 0 {
		batchTimeout = 3 * time.Second
	}
	batchCtx, cancel := context.WithTimeout(ctx, batchTimeout)
	defer cancel()

	for i := 1; i < c.config.Kafka.Consumer.MaxPollRecords; i++ {
		msg, err := c.reader.FetchMessage(batchCtx)
		if err != nil {
			break // timeout or error — return what we have
		}
		messages = append(messages, msg)
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

	// Group events by account ID for batch rate limit checking
	eventsByAccount := make(map[string][]kafka.Message)
	for _, message := range messages {
		var event dto.WebhookEventDTO
		if err := json.Unmarshal(message.Value, &event); err != nil {
			log.Printf("Failed to unmarshal event: %v", err)
			continue
		}
		eventsByAccount[event.AccountID] = append(eventsByAccount[event.AccountID], message)
	}

	// Perform batch rate limit checks per account
	var allowedEvents []kafka.Message
	for accountID, accountEvents := range eventsByAccount {
		if c.rateLimiter.AreEventsAllowed(ctx, accountID, len(accountEvents)) {
			allowedEvents = append(allowedEvents, accountEvents...)
		} else {
			// Republish rate-limited events back to Kafka
			log.Printf("Rate limited %d events for account %s", len(accountEvents), accountID)
			c.republishRateLimitedEvents(ctx, accountEvents)
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

// republishRateLimitedEvents republishes rate-limited events back to the Kafka topic
func (c *Consumer) republishRateLimitedEvents(ctx context.Context, messages []kafka.Message) {
	if c.eventProducer == nil {
		log.Printf("Event producer not set, cannot republish rate-limited events")
		return
	}

	topic := c.config.Kafka.Topics.WebhookEvent.Name
	for _, message := range messages {
		var event dto.WebhookEventDTO
		if err := json.Unmarshal(message.Value, &event); err != nil {
			log.Printf("Failed to unmarshal event for republish: %v", err)
			continue
		}
		if err := c.eventProducer.Publish(ctx, topic, string(message.Key), &event); err != nil {
			log.Printf("Failed to republish rate-limited event %s: %v", event.EventID, err)
		}
	}
}

// Close closes the Kafka consumer
func (c *Consumer) Close() error {
	return c.reader.Close()
}
