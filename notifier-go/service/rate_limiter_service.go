package service

import (
	"context"
)

// RateLimiterService defines the interface for rate limiting
type RateLimiterService interface {
	// IsAllowed checks if an account is allowed to process events based on rate limits
	IsAllowed(ctx context.Context, accountID string) bool
	
	// AreEventsAllowed checks if multiple events are allowed for an account
	AreEventsAllowed(ctx context.Context, accountID string, count int) bool
}
