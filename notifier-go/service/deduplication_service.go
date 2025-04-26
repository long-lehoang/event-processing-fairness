package service

import (
	"context"
)

// DeduplicationService defines the interface for event deduplication
type DeduplicationService interface {
	// IsDuplicate checks if an event has already been processed
	IsDuplicate(ctx context.Context, eventID string) bool
	
	// MarkProcessed marks an event as processed to prevent future duplicate processing
	MarkProcessed(ctx context.Context, eventID string) error
}
