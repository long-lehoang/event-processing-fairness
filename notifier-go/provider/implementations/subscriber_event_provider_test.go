package implementations

import (
	"context"
	"database/sql"
	"errors"
	"testing"
	"time"

	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/event-processing/notifier-go/domain/repository"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

// --- Mocks ---

type mockSubscriberCreatedEventRepo struct {
	mock.Mock
}

func (m *mockSubscriberCreatedEventRepo) FindPostUrlsByEventIDs(ctx context.Context, eventIDs []string) ([]repository.SubscriberPostURL, error) {
	args := m.Called(ctx, eventIDs)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).([]repository.SubscriberPostURL), args.Error(1)
}

func (m *mockSubscriberCreatedEventRepo) FetchEventsWithoutSegments(ctx context.Context, eventIDs []string) ([]repository.SubscriberEventRow, error) {
	args := m.Called(ctx, eventIDs)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).([]repository.SubscriberEventRow), args.Error(1)
}

type mockSegmentRepo struct {
	mock.Mock
}

func (m *mockSegmentRepo) FetchSegments(ctx context.Context, subscriberIDs []string) ([]repository.SegmentRow, error) {
	args := m.Called(ctx, subscriberIDs)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).([]repository.SegmentRow), args.Error(1)
}

// --- Tests ---

func TestSupports_SubscriberCreated_ReturnsTrue(t *testing.T) {
	p := NewSubscriberEventProvider(nil, nil)
	assert.True(t, p.Supports("subscriber.created"))
}

func TestSupports_OtherEventType_ReturnsFalse(t *testing.T) {
	p := NewSubscriberEventProvider(nil, nil)
	assert.False(t, p.Supports("order.completed"))
	assert.False(t, p.Supports("subscriber"))
	assert.False(t, p.Supports(""))
}

func TestGetWebhookUrls_Success(t *testing.T) {
	eventRepo := new(mockSubscriberCreatedEventRepo)
	p := NewSubscriberEventProvider(eventRepo, nil)
	ctx := context.Background()

	eventRepo.On("FindPostUrlsByEventIDs", ctx, []string{"e1", "e2"}).Return(
		[]repository.SubscriberPostURL{
			{EventID: "e1", PostURL: "https://hook1.com"},
			{EventID: "e2", PostURL: "https://hook2.com"},
		}, nil,
	)

	result, err := p.GetWebhookUrls(ctx, []string{"e1", "e2"})

	require.NoError(t, err)
	assert.Equal(t, "https://hook1.com", result["e1"])
	assert.Equal(t, "https://hook2.com", result["e2"])
}

func TestGetWebhookUrls_RepoError(t *testing.T) {
	eventRepo := new(mockSubscriberCreatedEventRepo)
	p := NewSubscriberEventProvider(eventRepo, nil)
	ctx := context.Background()

	eventRepo.On("FindPostUrlsByEventIDs", ctx, []string{"e1"}).Return(nil, errors.New("db error"))

	result, err := p.GetWebhookUrls(ctx, []string{"e1"})

	assert.Error(t, err)
	assert.Nil(t, result)
	assert.Contains(t, err.Error(), "db error")
}

func TestGetPayloads_Success_WithSegments(t *testing.T) {
	eventRepo := new(mockSubscriberCreatedEventRepo)
	segmentRepo := new(mockSegmentRepo)
	p := NewSubscriberEventProvider(eventRepo, segmentRepo)
	ctx := context.Background()

	eventTime := time.Now()
	createdAt := time.Now()

	eventRepo.On("FetchEventsWithoutSegments", ctx, []string{"e1"}).Return(
		[]repository.SubscriberEventRow{
			{
				ID:                  "e1",
				EventName:           sql.NullString{String: "subscriber.created", Valid: true},
				EventTime:           &eventTime,
				WebhookID:           "wh-1",
				SubscriberID:        "sub-1",
				SubscriberStatus:    sql.NullString{String: "active", Valid: true},
				SubscriberEmail:     "test@example.com",
				SubscriberSource:    sql.NullString{String: "api", Valid: true},
				FirstName:           sql.NullString{String: "John", Valid: true},
				LastName:            sql.NullString{String: "Doe", Valid: true},
				CustomFields:        sql.NullString{String: `{"key":"val"}`, Valid: true},
				OptinIp:             sql.NullString{String: "1.2.3.4", Valid: true},
				OptinTimestamp:      &createdAt,
				SubscriberCreatedAt: &createdAt,
			},
		}, nil,
	)
	segmentRepo.On("FetchSegments", ctx, mock.AnythingOfType("[]string")).Return(
		[]repository.SegmentRow{
			{ID: "seg-1", Name: "VIP", SubscriberID: "sub-1"},
			{ID: "seg-2", Name: "Premium", SubscriberID: "sub-1"},
		}, nil,
	)

	result, err := p.GetPayloads(ctx, []string{"e1"})

	require.NoError(t, err)
	require.Contains(t, result, "e1")

	event := result["e1"].(*dto.SubscriberEventDTO)
	assert.Equal(t, "e1", event.ID)
	assert.Equal(t, "subscriber.created", event.EventName)
	assert.Equal(t, "sub-1", event.Subscriber.ID)
	assert.Equal(t, "test@example.com", event.Subscriber.Email)
	assert.Equal(t, "John", event.Subscriber.FirstName)
	assert.Len(t, event.Subscriber.Segments, 2)
}

func TestGetPayloads_Success_NoSegments(t *testing.T) {
	eventRepo := new(mockSubscriberCreatedEventRepo)
	segmentRepo := new(mockSegmentRepo)
	p := NewSubscriberEventProvider(eventRepo, segmentRepo)
	ctx := context.Background()

	eventRepo.On("FetchEventsWithoutSegments", ctx, []string{"e1"}).Return(
		[]repository.SubscriberEventRow{
			{
				ID:               "e1",
				EventName:        sql.NullString{String: "subscriber.created", Valid: true},
				WebhookID:        "wh-1",
				SubscriberID:     "sub-1",
				SubscriberEmail:  "test@example.com",
				SubscriberStatus: sql.NullString{String: "active", Valid: true},
			},
		}, nil,
	)
	segmentRepo.On("FetchSegments", ctx, mock.AnythingOfType("[]string")).Return(
		[]repository.SegmentRow{}, nil,
	)

	result, err := p.GetPayloads(ctx, []string{"e1"})

	require.NoError(t, err)
	event := result["e1"].(*dto.SubscriberEventDTO)
	// Should be empty slice, not nil
	assert.NotNil(t, event.Subscriber.Segments)
	assert.Empty(t, event.Subscriber.Segments)
}

func TestGetPayloads_RepoError_EventFetch(t *testing.T) {
	eventRepo := new(mockSubscriberCreatedEventRepo)
	p := NewSubscriberEventProvider(eventRepo, nil)
	ctx := context.Background()

	eventRepo.On("FetchEventsWithoutSegments", ctx, []string{"e1"}).Return(nil, errors.New("db error"))

	result, err := p.GetPayloads(ctx, []string{"e1"})

	assert.Error(t, err)
	assert.Nil(t, result)
}

func TestGetPayloads_RepoError_SegmentFetch(t *testing.T) {
	eventRepo := new(mockSubscriberCreatedEventRepo)
	segmentRepo := new(mockSegmentRepo)
	p := NewSubscriberEventProvider(eventRepo, segmentRepo)
	ctx := context.Background()

	eventRepo.On("FetchEventsWithoutSegments", ctx, []string{"e1"}).Return(
		[]repository.SubscriberEventRow{
			{ID: "e1", SubscriberID: "sub-1", EventName: sql.NullString{String: "subscriber.created", Valid: true}},
		}, nil,
	)
	segmentRepo.On("FetchSegments", ctx, mock.AnythingOfType("[]string")).Return(nil, errors.New("segment db error"))

	result, err := p.GetPayloads(ctx, []string{"e1"})

	assert.Error(t, err)
	assert.Nil(t, result)
}

func TestGetPayloads_NilTimestamps(t *testing.T) {
	eventRepo := new(mockSubscriberCreatedEventRepo)
	segmentRepo := new(mockSegmentRepo)
	p := NewSubscriberEventProvider(eventRepo, segmentRepo)
	ctx := context.Background()

	eventRepo.On("FetchEventsWithoutSegments", ctx, []string{"e1"}).Return(
		[]repository.SubscriberEventRow{
			{
				ID:                  "e1",
				EventName:           sql.NullString{String: "subscriber.created", Valid: true},
				EventTime:           nil,
				WebhookID:           "wh-1",
				SubscriberID:        "sub-1",
				SubscriberEmail:     "test@example.com",
				OptinTimestamp:      nil,
				SubscriberCreatedAt: nil,
			},
		}, nil,
	)
	segmentRepo.On("FetchSegments", ctx, mock.AnythingOfType("[]string")).Return(
		[]repository.SegmentRow{}, nil,
	)

	result, err := p.GetPayloads(ctx, []string{"e1"})

	require.NoError(t, err)
	event := result["e1"].(*dto.SubscriberEventDTO)
	assert.Equal(t, "", event.EventTime)
	assert.Equal(t, "", event.Subscriber.OptinTimestamp)
	assert.Equal(t, "", event.Subscriber.CreatedAt)
}
