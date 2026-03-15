package service

import (
	"context"
	"fmt"

	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/event-processing/notifier-go/provider"
)

// WebhookEventServiceImpl implements the WebhookEventService interface
// by delegating to the appropriate event provider based on event type
type WebhookEventServiceImpl struct {
	providers []provider.WebhookEventProvider
}

// NewWebhookEventService creates a new WebhookEventServiceImpl
func NewWebhookEventService(providers []provider.WebhookEventProvider) *WebhookEventServiceImpl {
	return &WebhookEventServiceImpl{
		providers: providers,
	}
}

// GetWebhookUrls retrieves webhook URLs for a set of events by delegating to the matching provider
func (s *WebhookEventServiceImpl) GetWebhookUrls(ctx context.Context, eventType string, eventIDs []string) (map[string]string, error) {
	for _, p := range s.providers {
		if p.Supports(eventType) {
			return p.GetWebhookUrls(ctx, eventIDs)
		}
	}
	return nil, fmt.Errorf("unsupported event type: %s", eventType)
}

// GetPayloads retrieves event payloads for a set of events by delegating to the matching provider
func (s *WebhookEventServiceImpl) GetPayloads(ctx context.Context, eventType string, eventIDs []string) (map[string]dto.BaseEventDTO, error) {
	for _, p := range s.providers {
		if p.Supports(eventType) {
			return p.GetPayloads(ctx, eventIDs)
		}
	}
	return nil, fmt.Errorf("unsupported event type: %s", eventType)
}
