package service

import (
	"context"

	"github.com/event-processing/notifier-go/domain/dto"
)

// WebhookService defines the interface for webhook notification processing
type WebhookService interface {
	// ProcessWithRetry processes a webhook notification with retry capabilities
	ProcessWithRetry(ctx context.Context, eventID string, eventPayload *dto.WebhookEventDTO, url string, payload dto.BaseEventDTO) error
}

// WebhookClient defines the interface for webhook delivery functionality
type WebhookClient interface {
	// SendWebhook sends a webhook notification to the specified URL
	SendWebhook(ctx context.Context, url string, payload dto.BaseEventDTO) (bool, error)
}

// DeadLetterQueueProducer defines the interface for publishing failed events to a dead letter queue
type DeadLetterQueueProducer interface {
	// Publish publishes a failed event to the dead letter queue
	Publish(ctx context.Context, topic, key string, payload *dto.DeadLetterQueueEventDTO) error
}

// MetricsService defines the interface for metrics collection
type MetricsService interface {
	// IncrementWebhookSuccess increments the webhook success counter
	IncrementWebhookSuccess()

	// IncrementWebhookFailure increments the webhook failure counter
	IncrementWebhookFailure()

	// IncrementCircuitBreakerOpenCount increments the circuit breaker open counter
	IncrementCircuitBreakerOpenCount()
}

