package kafka

import (
	"context"
	"encoding/json"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"github.com/event-processing/notifier-go/config"
	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/segmentio/kafka-go"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

// --- Mocks ---

type mockEventProcessing struct {
	mock.Mock
}

func (m *mockEventProcessing) Process(ctx context.Context, eventID string, eventPayload *dto.WebhookEventDTO, url string, payload dto.BaseEventDTO) error {
	args := m.Called(ctx, eventID, eventPayload, url, payload)
	return args.Error(0)
}

type mockRateLimiter struct {
	mock.Mock
}

func (m *mockRateLimiter) IsAllowed(ctx context.Context, accountID string) bool {
	args := m.Called(ctx, accountID)
	return args.Bool(0)
}

func (m *mockRateLimiter) AreEventsAllowed(ctx context.Context, accountID string, count int) bool {
	args := m.Called(ctx, accountID, count)
	return args.Bool(0)
}

type mockWebhookEventService struct {
	mock.Mock
}

func (m *mockWebhookEventService) GetWebhookUrls(ctx context.Context, eventType string, eventIDs []string) (map[string]string, error) {
	args := m.Called(ctx, eventType, eventIDs)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(map[string]string), args.Error(1)
}

func (m *mockWebhookEventService) GetPayloads(ctx context.Context, eventType string, eventIDs []string) (map[string]dto.BaseEventDTO, error) {
	args := m.Called(ctx, eventType, eventIDs)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(map[string]dto.BaseEventDTO), args.Error(1)
}

type mockConsumerMetrics struct {
	mock.Mock
}

func (m *mockConsumerMetrics) IncrementKafkaEventCount(count int) {
	m.Called(count)
}

func (m *mockConsumerMetrics) RecordBatchProcessingTime(duration time.Duration) {
	m.Called(duration)
}

type mockConsumerEventProducer struct {
	mock.Mock
}

func (m *mockConsumerEventProducer) Publish(ctx context.Context, topic, key string, payload *dto.WebhookEventDTO) error {
	args := m.Called(ctx, topic, key, payload)
	return args.Error(0)
}

// --- Helpers ---

func newTestConsumer() *Consumer {
	cfg := &config.Config{}
	cfg.Kafka.Topics.WebhookEvent.Name = "webhook-events"
	cfg.ThreadPool.KafkaConsumer.MaxSize = 10
	return &Consumer{
		config:     cfg,
		workerPool: NewWorkerPool(10),
	}
}

func makeMessage(eventID, eventType, accountID string) kafka.Message {
	event := dto.WebhookEventDTO{
		EventID:   eventID,
		EventType: eventType,
		AccountID: accountID,
	}
	data, _ := json.Marshal(event)
	return kafka.Message{
		Key:   []byte(accountID),
		Value: data,
	}
}

// --- Tests ---

func TestStart_MissingEventProcessing_ReturnsError(t *testing.T) {
	c := newTestConsumer()
	c.rateLimiter = new(mockRateLimiter)
	c.eventService = new(mockWebhookEventService)
	c.metrics = new(mockConsumerMetrics)

	err := c.Start(context.Background())
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "event processing service not set")
}

func TestStart_MissingRateLimiter_ReturnsError(t *testing.T) {
	c := newTestConsumer()
	c.eventProcessing = new(mockEventProcessing)
	c.eventService = new(mockWebhookEventService)
	c.metrics = new(mockConsumerMetrics)

	err := c.Start(context.Background())
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "rate limiter service not set")
}

func TestStart_MissingEventService_ReturnsError(t *testing.T) {
	c := newTestConsumer()
	c.eventProcessing = new(mockEventProcessing)
	c.rateLimiter = new(mockRateLimiter)
	c.metrics = new(mockConsumerMetrics)

	err := c.Start(context.Background())
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "event service not set")
}

func TestStart_MissingMetrics_ReturnsError(t *testing.T) {
	c := newTestConsumer()
	c.eventProcessing = new(mockEventProcessing)
	c.rateLimiter = new(mockRateLimiter)
	c.eventService = new(mockWebhookEventService)

	err := c.Start(context.Background())
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "metrics service not set")
}

func TestProcessEventGroup_AllEventsAllowed(t *testing.T) {
	c := newTestConsumer()
	ep := new(mockEventProcessing)
	rl := new(mockRateLimiter)
	es := new(mockWebhookEventService)
	c.eventProcessing = ep
	c.rateLimiter = rl
	c.eventService = es

	ctx := context.Background()
	messages := []kafka.Message{
		makeMessage("e1", "subscriber.created", "acc-1"),
		makeMessage("e2", "subscriber.created", "acc-1"),
	}

	rl.On("AreEventsAllowed", ctx, "acc-1", 2).Return(true)
	es.On("GetWebhookUrls", ctx, "subscriber.created", mock.AnythingOfType("[]string")).Return(
		map[string]string{"e1": "https://hook1.com", "e2": "https://hook2.com"}, nil,
	)
	es.On("GetPayloads", ctx, "subscriber.created", mock.AnythingOfType("[]string")).Return(
		map[string]dto.BaseEventDTO{
			"e1": &dto.SubscriberEventDTO{ID: "e1", EventName: "subscriber.created"},
			"e2": &dto.SubscriberEventDTO{ID: "e2", EventName: "subscriber.created"},
		}, nil,
	)
	ep.On("Process", ctx, mock.AnythingOfType("string"), mock.AnythingOfType("*dto.WebhookEventDTO"), mock.AnythingOfType("string"), mock.AnythingOfType("*dto.SubscriberEventDTO")).Return(nil)

	c.processEventGroup(ctx, "subscriber.created", messages)

	// Both events should be processed
	assert.Equal(t, 2, len(ep.Calls))
}

func TestProcessEventGroup_RateLimitedEvents_Republished(t *testing.T) {
	c := newTestConsumer()
	ep := new(mockEventProcessing)
	rl := new(mockRateLimiter)
	es := new(mockWebhookEventService)
	producer := new(mockConsumerEventProducer)
	c.eventProcessing = ep
	c.rateLimiter = rl
	c.eventService = es
	c.eventProducer = producer

	ctx := context.Background()
	messages := []kafka.Message{
		makeMessage("e1", "subscriber.created", "acc-1"),
		makeMessage("e2", "subscriber.created", "acc-1"),
	}

	rl.On("AreEventsAllowed", ctx, "acc-1", 2).Return(false)
	producer.On("Publish", ctx, "webhook-events", mock.AnythingOfType("string"), mock.AnythingOfType("*dto.WebhookEventDTO")).Return(nil)

	c.processEventGroup(ctx, "subscriber.created", messages)

	// Events should be republished, not processed
	ep.AssertNotCalled(t, "Process", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything)
	assert.Equal(t, 2, len(producer.Calls))
}

func TestProcessEventGroup_MixedAccounts_PartialRateLimiting(t *testing.T) {
	c := newTestConsumer()
	ep := new(mockEventProcessing)
	rl := new(mockRateLimiter)
	es := new(mockWebhookEventService)
	producer := new(mockConsumerEventProducer)
	c.eventProcessing = ep
	c.rateLimiter = rl
	c.eventService = es
	c.eventProducer = producer

	ctx := context.Background()
	messages := []kafka.Message{
		makeMessage("e1", "subscriber.created", "acc-1"),
		makeMessage("e2", "subscriber.created", "acc-2"),
	}

	rl.On("AreEventsAllowed", ctx, "acc-1", 1).Return(true)
	rl.On("AreEventsAllowed", ctx, "acc-2", 1).Return(false)
	producer.On("Publish", ctx, "webhook-events", mock.AnythingOfType("string"), mock.AnythingOfType("*dto.WebhookEventDTO")).Return(nil)
	es.On("GetWebhookUrls", ctx, "subscriber.created", mock.AnythingOfType("[]string")).Return(
		map[string]string{"e1": "https://hook1.com"}, nil,
	)
	es.On("GetPayloads", ctx, "subscriber.created", mock.AnythingOfType("[]string")).Return(
		map[string]dto.BaseEventDTO{"e1": &dto.SubscriberEventDTO{ID: "e1"}}, nil,
	)
	ep.On("Process", ctx, mock.AnythingOfType("string"), mock.AnythingOfType("*dto.WebhookEventDTO"), mock.AnythingOfType("string"), mock.AnythingOfType("*dto.SubscriberEventDTO")).Return(nil)

	c.processEventGroup(ctx, "subscriber.created", messages)

	// acc-1 event processed, acc-2 event republished
	assert.True(t, len(ep.Calls) >= 1)
	assert.Equal(t, 1, len(producer.Calls))
}

func TestProcessEventGroup_GetWebhookUrlsError_ReturnsEarly(t *testing.T) {
	c := newTestConsumer()
	ep := new(mockEventProcessing)
	rl := new(mockRateLimiter)
	es := new(mockWebhookEventService)
	c.eventProcessing = ep
	c.rateLimiter = rl
	c.eventService = es

	ctx := context.Background()
	messages := []kafka.Message{
		makeMessage("e1", "subscriber.created", "acc-1"),
	}

	rl.On("AreEventsAllowed", ctx, "acc-1", 1).Return(true)
	es.On("GetWebhookUrls", ctx, "subscriber.created", mock.AnythingOfType("[]string")).Return(nil, assert.AnError)

	c.processEventGroup(ctx, "subscriber.created", messages)

	ep.AssertNotCalled(t, "Process", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything)
}

func TestProcessEventGroup_MissingWebhookURL_SkipsEvent(t *testing.T) {
	c := newTestConsumer()
	ep := new(mockEventProcessing)
	rl := new(mockRateLimiter)
	es := new(mockWebhookEventService)
	c.eventProcessing = ep
	c.rateLimiter = rl
	c.eventService = es

	ctx := context.Background()
	messages := []kafka.Message{
		makeMessage("e1", "subscriber.created", "acc-1"),
	}

	rl.On("AreEventsAllowed", ctx, "acc-1", 1).Return(true)
	es.On("GetWebhookUrls", ctx, "subscriber.created", mock.AnythingOfType("[]string")).Return(
		map[string]string{}, nil, // No URL for e1
	)
	es.On("GetPayloads", ctx, "subscriber.created", mock.AnythingOfType("[]string")).Return(
		map[string]dto.BaseEventDTO{"e1": &dto.SubscriberEventDTO{ID: "e1"}}, nil,
	)

	c.processEventGroup(ctx, "subscriber.created", messages)

	ep.AssertNotCalled(t, "Process", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything)
}

func TestRepublishRateLimitedEvents_NilProducer(t *testing.T) {
	c := newTestConsumer()
	// eventProducer is nil

	messages := []kafka.Message{
		makeMessage("e1", "subscriber.created", "acc-1"),
	}

	// Should not panic
	assert.NotPanics(t, func() {
		c.republishRateLimitedEvents(context.Background(), messages)
	})
}

func TestWorkerPool_Submit(t *testing.T) {
	pool := NewWorkerPool(3)
	var counter int64
	var wg sync.WaitGroup

	for i := 0; i < 10; i++ {
		wg.Add(1)
		pool.Submit(func() {
			defer wg.Done()
			atomic.AddInt64(&counter, 1)
		})
	}

	wg.Wait()
	assert.Equal(t, int64(10), atomic.LoadInt64(&counter))
}
