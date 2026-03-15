package impl

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/event-processing/notifier-go/config"
	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

// --- Mocks ---

type mockWebhookClient struct {
	mock.Mock
}

func (m *mockWebhookClient) SendWebhook(ctx context.Context, url string, payload dto.BaseEventDTO) (bool, error) {
	args := m.Called(ctx, url, payload)
	return args.Bool(0), args.Error(1)
}

type mockDLQProducer struct {
	mock.Mock
}

func (m *mockDLQProducer) Publish(ctx context.Context, topic, key string, payload *dto.DeadLetterQueueEventDTO) error {
	args := m.Called(ctx, topic, key, payload)
	return args.Error(0)
}

type mockMetrics struct {
	mock.Mock
}

func (m *mockMetrics) IncrementWebhookSuccess() {
	m.Called()
}

func (m *mockMetrics) IncrementWebhookFailure() {
	m.Called()
}

func (m *mockMetrics) IncrementCircuitBreakerOpenCount() {
	m.Called()
}

// --- Helpers ---

func testConfig() *config.Config {
	cfg := &config.Config{}
	cfg.Resilience.Retry.WebhookRetry.MaxAttempts = 3
	cfg.Resilience.Retry.WebhookRetry.WaitDuration = 50 * time.Millisecond
	cfg.Resilience.Retry.WebhookRetry.ExponentialBackoffMultiplier = 1.1
	cfg.Resilience.CircuitBreaker.WebhookCircuitBreaker.FailureRateThreshold = 50.0
	cfg.Resilience.CircuitBreaker.WebhookCircuitBreaker.SlowCallRateThreshold = 60.0
	cfg.Resilience.CircuitBreaker.WebhookCircuitBreaker.SlowCallDurationThreshold = 2 * time.Second
	cfg.Resilience.CircuitBreaker.WebhookCircuitBreaker.WaitDurationInOpenState = 100 * time.Millisecond
	cfg.Resilience.CircuitBreaker.WebhookCircuitBreaker.PermittedNumberOfCallsInHalfOpenState = 1
	cfg.Resilience.CircuitBreaker.WebhookCircuitBreaker.MinimumNumberOfCalls = 10
	cfg.Kafka.Topics.DeadLetterQueue.Name = "test-dlq-topic"
	return cfg
}

func testEventPayload() *dto.WebhookEventDTO {
	return &dto.WebhookEventDTO{
		EventID:   "event-1",
		EventType: "subscriber.created",
		AccountID: "account-1",
	}
}

func testPayload() dto.BaseEventDTO {
	return &dto.SubscriberEventDTO{
		ID:        "event-1",
		EventName: "subscriber.created",
		EventTime: "2023-01-01T00:00:00Z",
	}
}

// --- Tests ---

func TestProcessWithRetry_Success(t *testing.T) {
	client := new(mockWebhookClient)
	dlq := new(mockDLQProducer)
	metrics := new(mockMetrics)
	cfg := testConfig()

	svc := NewWebhookService(client, dlq, metrics, cfg)
	ctx := context.Background()
	payload := testPayload()

	client.On("SendWebhook", ctx, "https://example.com/hook", payload).Return(true, nil)
	metrics.On("IncrementWebhookSuccess").Return()

	err := svc.ProcessWithRetry(ctx, "event-1", testEventPayload(), "https://example.com/hook", payload)

	assert.NoError(t, err)
	metrics.AssertCalled(t, "IncrementWebhookSuccess")
	metrics.AssertNotCalled(t, "IncrementWebhookFailure")
	dlq.AssertNotCalled(t, "Publish", mock.Anything, mock.Anything, mock.Anything, mock.Anything)
}

func TestProcessWithRetry_WebhookReturnsFalse_SendsToDLQ(t *testing.T) {
	client := new(mockWebhookClient)
	dlq := new(mockDLQProducer)
	metrics := new(mockMetrics)
	cfg := testConfig()

	svc := NewWebhookService(client, dlq, metrics, cfg)
	ctx := context.Background()
	payload := testPayload()

	client.On("SendWebhook", ctx, "https://example.com/hook", payload).Return(false, nil)
	metrics.On("IncrementWebhookFailure").Return()
	dlq.On("Publish", ctx, "test-dlq-topic", "event-1", mock.AnythingOfType("*dto.DeadLetterQueueEventDTO")).Return(nil)

	err := svc.ProcessWithRetry(ctx, "event-1", testEventPayload(), "https://example.com/hook", payload)

	assert.Error(t, err)
	metrics.AssertCalled(t, "IncrementWebhookFailure")
	dlq.AssertCalled(t, "Publish", ctx, "test-dlq-topic", "event-1", mock.AnythingOfType("*dto.DeadLetterQueueEventDTO"))
}

func TestProcessWithRetry_WebhookReturnsError_SendsToDLQ(t *testing.T) {
	client := new(mockWebhookClient)
	dlq := new(mockDLQProducer)
	metrics := new(mockMetrics)
	cfg := testConfig()

	svc := NewWebhookService(client, dlq, metrics, cfg)
	ctx := context.Background()
	payload := testPayload()

	client.On("SendWebhook", ctx, "https://example.com/hook", payload).Return(false, errors.New("connection refused"))
	metrics.On("IncrementWebhookFailure").Return()
	dlq.On("Publish", ctx, "test-dlq-topic", "event-1", mock.AnythingOfType("*dto.DeadLetterQueueEventDTO")).Return(nil)

	err := svc.ProcessWithRetry(ctx, "event-1", testEventPayload(), "https://example.com/hook", payload)

	assert.Error(t, err)
	metrics.AssertCalled(t, "IncrementWebhookFailure")
	dlq.AssertCalled(t, "Publish", ctx, "test-dlq-topic", "event-1", mock.AnythingOfType("*dto.DeadLetterQueueEventDTO"))
}

func TestProcessWithRetry_SucceedsOnRetry(t *testing.T) {
	client := new(mockWebhookClient)
	dlq := new(mockDLQProducer)
	metrics := new(mockMetrics)
	cfg := testConfig()

	svc := NewWebhookService(client, dlq, metrics, cfg)
	ctx := context.Background()
	payload := testPayload()

	// Fail twice, succeed on third attempt
	client.On("SendWebhook", ctx, "https://example.com/hook", payload).Return(false, nil).Once()
	client.On("SendWebhook", ctx, "https://example.com/hook", payload).Return(false, nil).Once()
	client.On("SendWebhook", ctx, "https://example.com/hook", payload).Return(true, nil).Once()
	metrics.On("IncrementWebhookSuccess").Return()

	err := svc.ProcessWithRetry(ctx, "event-1", testEventPayload(), "https://example.com/hook", payload)

	assert.NoError(t, err)
	metrics.AssertCalled(t, "IncrementWebhookSuccess")
	dlq.AssertNotCalled(t, "Publish", mock.Anything, mock.Anything, mock.Anything, mock.Anything)
}

func TestProcessWithRetry_DLQPublishFailure_OriginalErrorReturned(t *testing.T) {
	client := new(mockWebhookClient)
	dlq := new(mockDLQProducer)
	metrics := new(mockMetrics)
	cfg := testConfig()

	svc := NewWebhookService(client, dlq, metrics, cfg)
	ctx := context.Background()
	payload := testPayload()

	client.On("SendWebhook", ctx, "https://example.com/hook", payload).Return(false, nil)
	metrics.On("IncrementWebhookFailure").Return()
	dlq.On("Publish", ctx, "test-dlq-topic", "event-1", mock.AnythingOfType("*dto.DeadLetterQueueEventDTO")).Return(errors.New("DLQ unavailable"))

	err := svc.ProcessWithRetry(ctx, "event-1", testEventPayload(), "https://example.com/hook", payload)

	// The original webhook error is returned, not the DLQ error
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "webhook response failed")
}

func TestProcessWithRetry_DLQEvent_HasCorrectFields(t *testing.T) {
	client := new(mockWebhookClient)
	dlq := new(mockDLQProducer)
	metrics := new(mockMetrics)
	cfg := testConfig()

	svc := NewWebhookService(client, dlq, metrics, cfg)
	ctx := context.Background()
	payload := testPayload()
	eventPayload := testEventPayload()

	client.On("SendWebhook", ctx, "https://example.com/hook", payload).Return(false, nil)
	metrics.On("IncrementWebhookFailure").Return()
	dlq.On("Publish", ctx, "test-dlq-topic", "event-1", mock.MatchedBy(func(d *dto.DeadLetterQueueEventDTO) bool {
		return d.EventID == "event-1" &&
			d.AccountID == "account-1" &&
			d.EventType == "subscriber.created" &&
			d.FailureReason == "Webhook delivery failed after retries"
	})).Return(nil)

	_ = svc.ProcessWithRetry(ctx, "event-1", eventPayload, "https://example.com/hook", payload)

	dlq.AssertExpectations(t)
}

func TestGetCircuitBreaker_SameURL_ReturnsSameBreaker(t *testing.T) {
	client := new(mockWebhookClient)
	dlq := new(mockDLQProducer)
	metrics := new(mockMetrics)
	cfg := testConfig()

	svc := NewWebhookService(client, dlq, metrics, cfg)

	cb1 := svc.getCircuitBreaker("https://example.com/hook")
	cb2 := svc.getCircuitBreaker("https://example.com/hook")

	assert.Same(t, cb1, cb2)
}

func TestGetCircuitBreaker_DifferentURLs_DifferentBreakers(t *testing.T) {
	client := new(mockWebhookClient)
	dlq := new(mockDLQProducer)
	metrics := new(mockMetrics)
	cfg := testConfig()

	svc := NewWebhookService(client, dlq, metrics, cfg)

	cb1 := svc.getCircuitBreaker("https://example.com/hook1")
	cb2 := svc.getCircuitBreaker("https://example.com/hook2")

	assert.NotSame(t, cb1, cb2)
}
