package redis

import (
	"context"
	"fmt"
	"log"

	"github.com/event-processing/notifier-go/config"
	"github.com/go-redis/redis/v8"
)

// Lua script for atomically checking and incrementing rate limit counters.
// Ported from Java's AccountRateLimiterServiceImpl.
const checkAndIncrementScript = `
local key = KEYS[1]
local count = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local ttl = tonumber(ARGV[3])
local exists = redis.call('EXISTS', key)
if exists == 0 then
  redis.call('SET', key, count, 'EX', ttl)
  return 1
else
  local current = tonumber(redis.call('GET', key))
  if (current + count) <= limit then
    redis.call('INCRBY', key, count)
    local keyTTL = redis.call('TTL', key)
    if keyTTL == -1 then
      redis.call('EXPIRE', key, ttl)
    end
    return 1
  else
    return 0
  end
end
`

// RateLimiterService provides rate limiting functionality
type RateLimiterService struct {
	client         *redis.Client
	eventLimit     int
	timeWindowMins int
	script         *redis.Script
}

// NewRateLimiterService creates a new RateLimiterService
func NewRateLimiterService(redisClient *Client, cfg *config.Config) *RateLimiterService {
	return &RateLimiterService{
		client:         redisClient.GetClient(),
		eventLimit:     cfg.Redis.Limit.Event,
		timeWindowMins: cfg.Redis.Limit.Time,
		script:         redis.NewScript(checkAndIncrementScript),
	}
}

// IsAllowed checks if an account is allowed to process events based on rate limits
func (s *RateLimiterService) IsAllowed(ctx context.Context, accountID string) bool {
	return s.AreEventsAllowed(ctx, accountID, 1)
}

// AreEventsAllowed checks if multiple events are allowed for an account using an atomic Lua script
func (s *RateLimiterService) AreEventsAllowed(ctx context.Context, accountID string, count int) bool {
	key := fmt.Sprintf("rate-limit:%s", accountID)
	expirySeconds := s.timeWindowMins * 60

	result, err := s.script.Run(ctx, s.client, []string{key},
		count, s.eventLimit, expirySeconds,
	).Int64()

	if err != nil {
		log.Printf("Error checking rate limit for account %s: %v", accountID, err)
		return true // Fail open on error
	}

	allowed := result == 1
	if !allowed {
		log.Printf("Rate limit exceeded for account %s: attempted %d events, limit %d", accountID, count, s.eventLimit)
	}

	return allowed
}
