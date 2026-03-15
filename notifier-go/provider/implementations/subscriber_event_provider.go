package implementations

import (
	"context"
	"fmt"
	"log"

	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/event-processing/notifier-go/domain/repository"
)

const subscriberCreatedEventType = "subscriber.created"

// SubscriberEventProvider implements the WebhookEventProvider interface for subscriber events
type SubscriberEventProvider struct {
	eventRepo   repository.SubscriberCreatedEventRepository
	segmentRepo repository.SegmentRepository
}

// NewSubscriberEventProvider creates a new SubscriberEventProvider
func NewSubscriberEventProvider(
	eventRepo repository.SubscriberCreatedEventRepository,
	segmentRepo repository.SegmentRepository,
) *SubscriberEventProvider {
	return &SubscriberEventProvider{
		eventRepo:   eventRepo,
		segmentRepo: segmentRepo,
	}
}

// Supports checks if this provider supports the given event type
func (p *SubscriberEventProvider) Supports(eventType string) bool {
	return eventType == subscriberCreatedEventType
}

// GetWebhookUrls retrieves webhook URLs for a set of subscriber events
func (p *SubscriberEventProvider) GetWebhookUrls(ctx context.Context, eventIDs []string) (map[string]string, error) {
	log.Printf("Getting webhook URLs for %d subscriber events", len(eventIDs))

	postURLs, err := p.eventRepo.FindPostUrlsByEventIDs(ctx, eventIDs)
	if err != nil {
		return nil, fmt.Errorf("failed to find post URLs: %w", err)
	}

	result := make(map[string]string, len(postURLs))
	for _, row := range postURLs {
		result[row.EventID] = row.PostURL
	}

	return result, nil
}

// GetPayloads retrieves event payloads for a set of subscriber events
func (p *SubscriberEventProvider) GetPayloads(ctx context.Context, eventIDs []string) (map[string]dto.BaseEventDTO, error) {
	log.Printf("Getting payloads for %d subscriber events", len(eventIDs))

	// Fetch events with subscriber data
	events, err := p.eventRepo.FetchEventsWithoutSegments(ctx, eventIDs)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch events: %w", err)
	}

	// Extract unique subscriber IDs
	subscriberIDSet := make(map[string]struct{})
	for _, e := range events {
		subscriberIDSet[e.SubscriberID] = struct{}{}
	}
	subscriberIDs := make([]string, 0, len(subscriberIDSet))
	for id := range subscriberIDSet {
		subscriberIDs = append(subscriberIDs, id)
	}

	// Fetch segments for all subscribers
	segmentMap, err := p.fetchSegmentsForSubscribers(ctx, subscriberIDs)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch segments: %w", err)
	}

	// Build result map
	result := make(map[string]dto.BaseEventDTO, len(events))
	for _, e := range events {
		optinTimestamp := ""
		if e.OptinTimestamp != nil {
			optinTimestamp = e.OptinTimestamp.String()
		}
		createdAt := ""
		if e.SubscriberCreatedAt != nil {
			createdAt = e.SubscriberCreatedAt.String()
		}
		eventTime := ""
		if e.EventTime != nil {
			eventTime = e.EventTime.String()
		}

		subscriber := dto.SubscriberDTO{
			ID:             e.SubscriberID,
			Status:         e.SubscriberStatus.String,
			Email:          e.SubscriberEmail,
			Source:         e.SubscriberSource.String,
			FirstName:      e.FirstName.String,
			LastName:       e.LastName.String,
			CustomFields:   e.CustomFields.String,
			OptinIp:        e.OptinIp.String,
			OptinTimestamp: optinTimestamp,
			CreatedAt:      createdAt,
			Segments:       segmentMap[e.SubscriberID],
		}

		if subscriber.Segments == nil {
			subscriber.Segments = []dto.SegmentDTO{}
		}

		event := &dto.SubscriberEventDTO{
			ID:         e.ID,
			EventName:  e.EventName.String,
			EventTime:  eventTime,
			Subscriber: subscriber,
			WebhookID:  e.WebhookID,
		}

		result[e.ID] = event
	}

	return result, nil
}

// fetchSegmentsForSubscribers fetches and groups segments by subscriber ID
func (p *SubscriberEventProvider) fetchSegmentsForSubscribers(ctx context.Context, subscriberIDs []string) (map[string][]dto.SegmentDTO, error) {
	if len(subscriberIDs) == 0 {
		return make(map[string][]dto.SegmentDTO), nil
	}

	segmentRows, err := p.segmentRepo.FetchSegments(ctx, subscriberIDs)
	if err != nil {
		return nil, err
	}

	result := make(map[string][]dto.SegmentDTO)
	for _, row := range segmentRows {
		segment := dto.SegmentDTO{
			ID:           row.ID,
			Name:         row.Name,
			SubscriberID: row.SubscriberID,
		}
		result[row.SubscriberID] = append(result[row.SubscriberID], segment)
	}

	return result, nil
}
