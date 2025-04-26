package redis

import (
	"context"
	"fmt"
	"log"
	"time"

	"github.com/event-processing/notifier-go/config"
	"github.com/go-redis/redis/v8"
)

// RateLimiterService provides rate limiting functionality
type RateLimiterService struct {
	client         *redis.Client
	eventLimit     int
	timeWindowMins int
}

// NewRateLimiterService creates a new RateLimiterService
func NewRateLimiterService(redisClient *Client, cfg *config.Config) *RateLimiterService {
	return &RateLimiterService{
		client:         redisClient.GetClient(),
		eventLimit:     cfg.Redis.Limit.Event,
		timeWindowMins: cfg.Redis.Limit.Time,
	}
}

// IsAllowed checks if an account is allowed to process events based on rate limits
func (s *RateLimiterService) IsAllowed(ctx context.Context, accountID string) bool {
	return s.AreEventsAllowed(ctx, accountID, 1)
}

// AreEventsAllowed checks if multiple events are allowed for an account
func (s *RateLimiterService) AreEventsAllowed(ctx context.Context, accountID string, count int) bool {
	key := fmt.Sprintf("rate:limit:%s", accountID)
	
	// Use Redis MULTI to execute commands atomically
	pipe := s.client.Pipeline()
	
	// Get current count
	countCmd := pipe.Get(ctx, key)
	
	// Increment count
	pipe.IncrBy(ctx, key, int64(count))
	
	// Set expiry if not already set
	pipe.Expire(ctx, key, time.Duration(s.timeWindowMins)*time.Minute)
	
	// Execute pipeline
	_, err := pipe.Exec(ctx)
	if err != nil && err != redis.Nil {
		log.Printf("Error checking rate limit for account %s: %v", accountID, err)
		return true // Fail open on error
	}
	
	// Get the value after increment
	val, err := countCmd.Int64()
	if err == redis.Nil {
		// Key doesn't exist yet, this is the first event
		return true
	} else if err != nil {
		log.Printf("Error getting rate limit count for account %s: %v", accountID, err)
		return true // Fail open on error
	}
	
	// Check if under limit
	allowed := val <= int64(s.eventLimit)
	if !allowed {
		log.Printf("Rate limit exceeded for account %s: %d/%d", accountID, val, s.eventLimit)
	}
	
	return allowed
}
