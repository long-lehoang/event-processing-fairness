package repository

import (
	"context"
	"database/sql"
	"fmt"

	"github.com/lib/pq"
)

// SegmentRow holds segment data joined with subscriber association
type SegmentRow struct {
	ID           string
	Name         string
	SubscriberID string
}

// SegmentRepository defines the interface for segment data access
type SegmentRepository interface {
	FetchSegments(ctx context.Context, subscriberIDs []string) ([]SegmentRow, error)
}

// segmentRepositoryImpl implements SegmentRepository
type segmentRepositoryImpl struct {
	db *sql.DB
}

// NewSegmentRepository creates a new SegmentRepository
func NewSegmentRepository(db *sql.DB) SegmentRepository {
	return &segmentRepositoryImpl{db: db}
}

// FetchSegments fetches segments associated with the given subscriber IDs
func (r *segmentRepositoryImpl) FetchSegments(ctx context.Context, subscriberIDs []string) ([]SegmentRow, error) {
	if len(subscriberIDs) == 0 {
		return nil, nil
	}

	query := `
		SELECT seg.id, seg.name, sub_seg.subscriber_id
		FROM subscriber_segment sub_seg
		JOIN segment seg ON sub_seg.segment_id = seg.id
		WHERE sub_seg.subscriber_id = ANY($1)
	`

	rows, err := r.db.QueryContext(ctx, query, pq.Array(subscriberIDs))
	if err != nil {
		return nil, fmt.Errorf("failed to query segments: %w", err)
	}
	defer rows.Close()

	var results []SegmentRow
	for rows.Next() {
		var row SegmentRow
		if err := rows.Scan(&row.ID, &row.Name, &row.SubscriberID); err != nil {
			return nil, fmt.Errorf("failed to scan segment row: %w", err)
		}
		results = append(results, row)
	}

	return results, rows.Err()
}
