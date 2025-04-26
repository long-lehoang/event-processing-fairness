package application

import (
	"context"
	"log"
	
	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/event-processing/notifier-go/service"
)

// WebhookEventProcessing defines the interface for webhook event processing
type WebhookEventProcessing interface {
	// Process processes a webhook event and delivers it to the specified URL
	Process(ctx context.Context, eventID string, eventPayload *dto.WebhookEventDTO, url string, payload dto.BaseEventDTO) error
}

// WebhookEventFairnessProcessing implements the WebhookEventProcessing interface
// with fairness-aware event processing
type WebhookEventFairnessProcessing struct {
	deduplicationService service.DeduplicationService
	webhookService       service.WebhookService
	eventProducer        EventProducer
	webhookEventTopic    string
	metrics              MetricsService
}

// EventProducer defines the interface for publishing webhook events
type EventProducer interface {
	// Publish publishes a webhook event to Kafka
	Publish(ctx context.Context, topic, key string, payload *dto.WebhookEventDTO) error
}

// MetricsService defines the interface for metrics collection
type MetricsService interface {
	// IncrementDuplicateEventCount increments the duplicate event counter
	IncrementDuplicateEventCount()
}

// NewWebhookEventFairnessProcessing creates a new WebhookEventFairnessProcessing
func NewWebhookEventFairnessProcessing(
	deduplicationService service.DeduplicationService,
	webhookService service.WebhookService,
	eventProducer EventProducer,
	webhookEventTopic string,
	metrics MetricsService,
) *WebhookEventFairnessProcessing {
	return &WebhookEventFairnessProcessing{
		deduplicationService: deduplicationService,
		webhookService:       webhookService,
		eventProducer:        eventProducer,
		webhookEventTopic:    webhookEventTopic,
		metrics:              metrics,
	}
}

// Process processes a webhook event and delivers it to the specified URL
func (p *WebhookEventFairnessProcessing) Process(
	ctx context.Context,
	eventID string,
	eventPayload *dto.WebhookEventDTO,
	url string,
	payload dto.BaseEventDTO,
) error {
	// Check for duplicate events
	if p.deduplicationService.IsDuplicate(ctx, eventID) {
		log.Printf("Skipping duplicate event: %s", eventID)
		p.metrics.IncrementDuplicateEventCount()
		return nil
	}
	
	// Mark the event as processed to prevent duplicate processing
	if err := p.deduplicationService.MarkProcessed(ctx, eventID); err != nil {
		log.Printf("Failed to mark event as processed: %v", err)
		// Continue processing even if marking fails
	}
	
	// Process the webhook with retry
	if err := p.webhookService.ProcessWithRetry(ctx, eventID, eventPayload, url, payload); err != nil {
		log.Printf("Failed to process webhook for event %s: %v", eventID, err)
		return err
	}
	
	log.Printf("Successfully processed webhook for event %s", eventID)
	return nil
}
