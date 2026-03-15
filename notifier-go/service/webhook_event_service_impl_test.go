package service

import (
	"context"
	"errors"
	"testing"

	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/event-processing/notifier-go/provider"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// --- Mock ---

type mockProvider struct {
	supportedType string
	urlsResult    map[string]string
	urlsErr       error
	payloadsResult map[string]dto.BaseEventDTO
	payloadsErr    error
}

func (m *mockProvider) Supports(eventType string) bool {
	return eventType == m.supportedType
}

func (m *mockProvider) GetWebhookUrls(ctx context.Context, eventIDs []string) (map[string]string, error) {
	return m.urlsResult, m.urlsErr
}

func (m *mockProvider) GetPayloads(ctx context.Context, eventIDs []string) (map[string]dto.BaseEventDTO, error) {
	return m.payloadsResult, m.payloadsErr
}

// --- Tests ---

func TestGetWebhookUrls_MatchingProvider(t *testing.T) {
	p := &mockProvider{
		supportedType: "subscriber.created",
		urlsResult:    map[string]string{"e1": "https://hook.com"},
	}
	svc := NewWebhookEventService([]provider.WebhookEventProvider{p})

	result, err := svc.GetWebhookUrls(context.Background(), "subscriber.created", []string{"e1"})

	require.NoError(t, err)
	assert.Equal(t, "https://hook.com", result["e1"])
}

func TestGetWebhookUrls_NoMatchingProvider(t *testing.T) {
	p := &mockProvider{supportedType: "subscriber.created"}
	svc := NewWebhookEventService([]provider.WebhookEventProvider{p})

	_, err := svc.GetWebhookUrls(context.Background(), "order.completed", []string{"e1"})

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "unsupported event type")
}

func TestGetWebhookUrls_MultipleProviders_FirstMatchWins(t *testing.T) {
	p1 := &mockProvider{
		supportedType: "subscriber.created",
		urlsResult:    map[string]string{"e1": "https://first.com"},
	}
	p2 := &mockProvider{
		supportedType: "subscriber.created",
		urlsResult:    map[string]string{"e1": "https://second.com"},
	}
	svc := NewWebhookEventService([]provider.WebhookEventProvider{p1, p2})

	result, err := svc.GetWebhookUrls(context.Background(), "subscriber.created", []string{"e1"})

	require.NoError(t, err)
	assert.Equal(t, "https://first.com", result["e1"])
}

func TestGetPayloads_MatchingProvider(t *testing.T) {
	payload := &dto.SubscriberEventDTO{ID: "e1", EventName: "subscriber.created"}
	p := &mockProvider{
		supportedType:  "subscriber.created",
		payloadsResult: map[string]dto.BaseEventDTO{"e1": payload},
	}
	svc := NewWebhookEventService([]provider.WebhookEventProvider{p})

	result, err := svc.GetPayloads(context.Background(), "subscriber.created", []string{"e1"})

	require.NoError(t, err)
	assert.Equal(t, payload, result["e1"])
}

func TestGetPayloads_NoMatchingProvider(t *testing.T) {
	p := &mockProvider{supportedType: "subscriber.created"}
	svc := NewWebhookEventService([]provider.WebhookEventProvider{p})

	_, err := svc.GetPayloads(context.Background(), "order.completed", []string{"e1"})

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "unsupported event type")
}

func TestGetPayloads_ProviderReturnsError(t *testing.T) {
	p := &mockProvider{
		supportedType: "subscriber.created",
		payloadsErr:   errors.New("db error"),
	}
	svc := NewWebhookEventService([]provider.WebhookEventProvider{p})

	_, err := svc.GetPayloads(context.Background(), "subscriber.created", []string{"e1"})

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "db error")
}
