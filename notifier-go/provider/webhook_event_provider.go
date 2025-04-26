package provider

import (
	"context"
	
	"github.com/event-processing/notifier-go/domain/dto"
)

// WebhookEventProvider defines the interface for webhook event providers
type WebhookEventProvider interface {
	// Supports checks if this provider supports the given event type
	Supports(eventType string) bool
	
	// GetWebhookUrls retrieves webhook URLs for a set of events
	GetWebhookUrls(ctx context.Context, eventIDs []string) (map[string]string, error)
	
	// GetPayloads retrieves event payloads for a set of events
	GetPayloads(ctx context.Context, eventIDs []string) (map[string]dto.BaseEventDTO, error)
}
