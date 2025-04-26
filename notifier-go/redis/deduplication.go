package redis

import (
	"context"
	"fmt"
	"log"
	"time"

	"github.com/go-redis/redis/v8"
)

const (
	// KeyPrefix is the prefix used for Redis keys to identify deduplication entries
	KeyPrefix = "deduplicate:event:"
	// ExpiryTime is the default expiry time for deduplication entries
	ExpiryTime = 24 * time.Hour
)

// DeduplicationService provides event deduplication functionality
type DeduplicationService struct {
	client *redis.Client
}

// NewDeduplicationService creates a new DeduplicationService
func NewDeduplicationService(redisClient *Client) *DeduplicationService {
	return &DeduplicationService{
		client: redisClient.GetClient(),
	}
}

// IsDuplicate checks if an event has already been processed
func (s *DeduplicationService) IsDuplicate(ctx context.Context, eventID string) bool {
	key := KeyPrefix + eventID
	exists, err := s.client.Exists(ctx, key).Result()
	if err != nil {
		log.Printf("Error checking duplicate for eventId %s: %v", eventID, err)
		return false // Fail open on error
	}
	
	log.Printf("Checking duplicate for eventId %s: %t", eventID, exists > 0)
	return exists > 0
}

// MarkProcessed marks an event as processed to prevent future duplicate processing
func (s *DeduplicationService) MarkProcessed(ctx context.Context, eventID string) error {
	key := KeyPrefix + eventID
	err := s.client.Set(ctx, key, "1", ExpiryTime).Err()
	if err != nil {
		return fmt.Errorf("failed to mark event %s as processed: %w", eventID, err)
	}
	
	log.Printf("Marked event %s as processed in Redis", eventID)
	return nil
}
