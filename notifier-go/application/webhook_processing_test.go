package application

import (
	"context"
	"errors"
	"testing"

	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

// --- Mocks ---

type mockDeduplicationService struct {
	mock.Mock
}

func (m *mockDeduplicationService) IsDuplicate(ctx context.Context, eventID string) bool {
	args := m.Called(ctx, eventID)
	return args.Bool(0)
}

func (m *mockDeduplicationService) MarkProcessed(ctx context.Context, eventID string) error {
	args := m.Called(ctx, eventID)
	return args.Error(0)
}

type mockWebhookService struct {
	mock.Mock
}

func (m *mockWebhookService) ProcessWithRetry(ctx context.Context, eventID string, eventPayload *dto.WebhookEventDTO, url string, payload dto.BaseEventDTO) error {
	args := m.Called(ctx, eventID, eventPayload, url, payload)
	return args.Error(0)
}

type mockEventProducer struct {
	mock.Mock
}

func (m *mockEventProducer) Publish(ctx context.Context, topic, key string, payload *dto.WebhookEventDTO) error {
	args := m.Called(ctx, topic, key, payload)
	return args.Error(0)
}

type mockMetricsService struct {
	mock.Mock
}

func (m *mockMetricsService) IncrementDuplicateEventCount() {
	m.Called()
}

func (m *mockMetricsService) IncrementWebhookExecutionCount() {
	m.Called()
}

// --- Test helpers ---

func newTestProcessing() (*WebhookEventFairnessProcessing, *mockDeduplicationService, *mockWebhookService, *mockEventProducer, *mockMetricsService) {
	dedupSvc := new(mockDeduplicationService)
	webhookSvc := new(mockWebhookService)
	producer := new(mockEventProducer)
	metrics := new(mockMetricsService)

	processing := NewWebhookEventFairnessProcessing(dedupSvc, webhookSvc, producer, "webhook-events", metrics)
	return processing, dedupSvc, webhookSvc, producer, metrics
}

func testEvent() (*dto.WebhookEventDTO, *dto.SubscriberEventDTO) {
	eventPayload := &dto.WebhookEventDTO{
		EventID:   "event-1",
		EventType: "subscriber.created",
		AccountID: "account-1",
	}
	payload := &dto.SubscriberEventDTO{
		ID:        "event-1",
		EventName: "subscriber.created",
		EventTime: "2023-01-01T00:00:00Z",
	}
	return eventPayload, payload
}

// --- Tests ---

func TestProcess_SuccessfulProcessing(t *testing.T) {
	processing, dedupSvc, webhookSvc, _, metrics := newTestProcessing()
	eventPayload, payload := testEvent()
	ctx := context.Background()

	dedupSvc.On("IsDuplicate", ctx, "event-1").Return(false)
	metrics.On("IncrementWebhookExecutionCount").Return()
	webhookSvc.On("ProcessWithRetry", ctx, "event-1", eventPayload, "https://example.com/webhook", payload).Return(nil)
	dedupSvc.On("MarkProcessed", ctx, "event-1").Return(nil)

	err := processing.Process(ctx, "event-1", eventPayload, "https://example.com/webhook", payload)

	assert.NoError(t, err)
	dedupSvc.AssertCalled(t, "IsDuplicate", ctx, "event-1")
	metrics.AssertCalled(t, "IncrementWebhookExecutionCount")
	webhookSvc.AssertCalled(t, "ProcessWithRetry", ctx, "event-1", eventPayload, "https://example.com/webhook", payload)
	dedupSvc.AssertCalled(t, "MarkProcessed", ctx, "event-1")
	metrics.AssertNotCalled(t, "IncrementDuplicateEventCount")
}

func TestProcess_DuplicateEvent_SkipsProcessing(t *testing.T) {
	processing, dedupSvc, webhookSvc, _, metrics := newTestProcessing()
	eventPayload, payload := testEvent()
	ctx := context.Background()

	dedupSvc.On("IsDuplicate", ctx, "event-1").Return(true)
	metrics.On("IncrementDuplicateEventCount").Return()

	err := processing.Process(ctx, "event-1", eventPayload, "https://example.com/webhook", payload)

	assert.NoError(t, err)
	dedupSvc.AssertCalled(t, "IsDuplicate", ctx, "event-1")
	metrics.AssertCalled(t, "IncrementDuplicateEventCount")
	webhookSvc.AssertNotCalled(t, "ProcessWithRetry", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything)
	dedupSvc.AssertNotCalled(t, "MarkProcessed", mock.Anything, mock.Anything)
}

func TestProcess_WebhookFailure_ReturnsError(t *testing.T) {
	processing, dedupSvc, webhookSvc, _, metrics := newTestProcessing()
	eventPayload, payload := testEvent()
	ctx := context.Background()

	dedupSvc.On("IsDuplicate", ctx, "event-1").Return(false)
	metrics.On("IncrementWebhookExecutionCount").Return()
	webhookSvc.On("ProcessWithRetry", ctx, "event-1", eventPayload, "https://example.com/webhook", payload).Return(errors.New("webhook failed"))

	err := processing.Process(ctx, "event-1", eventPayload, "https://example.com/webhook", payload)

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "webhook failed")
	dedupSvc.AssertNotCalled(t, "MarkProcessed", mock.Anything, mock.Anything)
}

func TestProcess_MarkProcessedFailure_StillReturnsNil(t *testing.T) {
	processing, dedupSvc, webhookSvc, _, metrics := newTestProcessing()
	eventPayload, payload := testEvent()
	ctx := context.Background()

	dedupSvc.On("IsDuplicate", ctx, "event-1").Return(false)
	metrics.On("IncrementWebhookExecutionCount").Return()
	webhookSvc.On("ProcessWithRetry", ctx, "event-1", eventPayload, "https://example.com/webhook", payload).Return(nil)
	dedupSvc.On("MarkProcessed", ctx, "event-1").Return(errors.New("redis error"))

	err := processing.Process(ctx, "event-1", eventPayload, "https://example.com/webhook", payload)

	// MarkProcessed failure is logged but does not propagate
	assert.NoError(t, err)
	dedupSvc.AssertCalled(t, "MarkProcessed", ctx, "event-1")
}

func TestNewWebhookEventFairnessProcessing(t *testing.T) {
	dedupSvc := new(mockDeduplicationService)
	webhookSvc := new(mockWebhookService)
	producer := new(mockEventProducer)
	metrics := new(mockMetricsService)

	processing := NewWebhookEventFairnessProcessing(dedupSvc, webhookSvc, producer, "test-topic", metrics)

	assert.NotNil(t, processing)
	assert.Equal(t, "test-topic", processing.webhookEventTopic)
}
