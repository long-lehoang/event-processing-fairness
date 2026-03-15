package repository

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"github.com/lib/pq"
)

// SubscriberPostURL holds the mapping of event ID to webhook post URL
type SubscriberPostURL struct {
	EventID string
	PostURL string
}

// SubscriberEventRow holds event data joined with subscriber data
type SubscriberEventRow struct {
	// Event fields
	ID        string
	EventName sql.NullString
	EventTime *time.Time
	WebhookID string
	// Subscriber fields
	SubscriberID        string
	SubscriberStatus    sql.NullString
	SubscriberEmail     string
	SubscriberSource    sql.NullString
	FirstName           sql.NullString
	LastName            sql.NullString
	CustomFields        sql.NullString
	OptinIp             sql.NullString
	OptinTimestamp      *time.Time
	SubscriberCreatedAt *time.Time
}

// SubscriberCreatedEventRepository defines the interface for subscriber created event data access
type SubscriberCreatedEventRepository interface {
	FindPostUrlsByEventIDs(ctx context.Context, eventIDs []string) ([]SubscriberPostURL, error)
	FetchEventsWithoutSegments(ctx context.Context, eventIDs []string) ([]SubscriberEventRow, error)
}

// subscriberCreatedEventRepositoryImpl implements SubscriberCreatedEventRepository
type subscriberCreatedEventRepositoryImpl struct {
	db *sql.DB
}

// NewSubscriberCreatedEventRepository creates a new SubscriberCreatedEventRepository
func NewSubscriberCreatedEventRepository(db *sql.DB) SubscriberCreatedEventRepository {
	return &subscriberCreatedEventRepositoryImpl{db: db}
}

// FindPostUrlsByEventIDs fetches webhook post URLs for the given event IDs
func (r *subscriberCreatedEventRepositoryImpl) FindPostUrlsByEventIDs(ctx context.Context, eventIDs []string) ([]SubscriberPostURL, error) {
	if len(eventIDs) == 0 {
		return nil, nil
	}

	query := `
		SELECT s.id, w.post_url
		FROM subscriber_created_event s
		JOIN webhook w ON s.webhook_id = w.id
		WHERE s.id = ANY($1)
	`

	rows, err := r.db.QueryContext(ctx, query, pq.Array(eventIDs))
	if err != nil {
		return nil, fmt.Errorf("failed to query post URLs: %w", err)
	}
	defer rows.Close()

	var results []SubscriberPostURL
	for rows.Next() {
		var row SubscriberPostURL
		if err := rows.Scan(&row.EventID, &row.PostURL); err != nil {
			return nil, fmt.Errorf("failed to scan post URL row: %w", err)
		}
		results = append(results, row)
	}

	return results, rows.Err()
}

// FetchEventsWithoutSegments fetches subscriber events with their associated subscriber information
func (r *subscriberCreatedEventRepositoryImpl) FetchEventsWithoutSegments(ctx context.Context, eventIDs []string) ([]SubscriberEventRow, error) {
	if len(eventIDs) == 0 {
		return nil, nil
	}

	query := `
		SELECT s.id, s.event_name, s.event_time,
		       sub.id, sub.status, sub.email, sub.source,
		       sub.first_name, sub.last_name, sub.custom_fields,
		       sub.optin_ip, sub.optin_timestamp, sub.created_at,
		       s.webhook_id
		FROM subscriber_created_event s
		JOIN subscriber sub ON s.subscriber_id = sub.id
		WHERE s.id = ANY($1)
	`

	rows, err := r.db.QueryContext(ctx, query, pq.Array(eventIDs))
	if err != nil {
		return nil, fmt.Errorf("failed to query events: %w", err)
	}
	defer rows.Close()

	var results []SubscriberEventRow
	for rows.Next() {
		var row SubscriberEventRow
		if err := rows.Scan(
			&row.ID, &row.EventName, &row.EventTime,
			&row.SubscriberID, &row.SubscriberStatus, &row.SubscriberEmail, &row.SubscriberSource,
			&row.FirstName, &row.LastName, &row.CustomFields,
			&row.OptinIp, &row.OptinTimestamp, &row.SubscriberCreatedAt,
			&row.WebhookID,
		); err != nil {
			return nil, fmt.Errorf("failed to scan event row: %w", err)
		}
		results = append(results, row)
	}

	return results, rows.Err()
}
