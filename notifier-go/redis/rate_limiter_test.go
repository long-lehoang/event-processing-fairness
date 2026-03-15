package redis

import (
	"context"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	goredis "github.com/go-redis/redis/v8"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func setupRateLimiterTest(t *testing.T, eventLimit int, timeWindowMins int) (*miniredis.Miniredis, *RateLimiterService) {
	mr, err := miniredis.Run()
	require.NoError(t, err)
	t.Cleanup(func() { mr.Close() })

	client := goredis.NewClient(&goredis.Options{
		Addr: mr.Addr(),
	})
	svc := &RateLimiterService{
		client:         client,
		eventLimit:     eventLimit,
		timeWindowMins: timeWindowMins,
		script:         goredis.NewScript(checkAndIncrementScript),
	}
	return mr, svc
}

func TestIsAllowed_FirstRequest_ReturnsTrue(t *testing.T) {
	mr, svc := setupRateLimiterTest(t, 5, 1)
	ctx := context.Background()

	result := svc.IsAllowed(ctx, "account-1")

	assert.True(t, result)

	// Verify key was set
	val, err := mr.Get("rate-limit:account-1")
	require.NoError(t, err)
	assert.Equal(t, "1", val)
}

func TestIsAllowed_WithinLimit_ReturnsTrue(t *testing.T) {
	_, svc := setupRateLimiterTest(t, 5, 1)
	ctx := context.Background()

	for i := 0; i < 4; i++ {
		result := svc.IsAllowed(ctx, "account-1")
		assert.True(t, result)
	}
}

func TestIsAllowed_ExceedsLimit_ReturnsFalse(t *testing.T) {
	_, svc := setupRateLimiterTest(t, 5, 1)
	ctx := context.Background()

	// Exhaust the limit
	for i := 0; i < 5; i++ {
		svc.IsAllowed(ctx, "account-1")
	}

	// 6th request should be denied
	result := svc.IsAllowed(ctx, "account-1")
	assert.False(t, result)
}

func TestAreEventsAllowed_BatchWithinLimit_ReturnsTrue(t *testing.T) {
	mr, svc := setupRateLimiterTest(t, 5, 1)
	ctx := context.Background()

	result := svc.AreEventsAllowed(ctx, "account-1", 3)

	assert.True(t, result)

	val, err := mr.Get("rate-limit:account-1")
	require.NoError(t, err)
	assert.Equal(t, "3", val)
}

func TestAreEventsAllowed_SecondBatchExceedsLimit_ReturnsFalse(t *testing.T) {
	_, svc := setupRateLimiterTest(t, 5, 1)
	ctx := context.Background()

	// First batch: 3 events, succeeds
	result := svc.AreEventsAllowed(ctx, "account-1", 3)
	assert.True(t, result)

	// Second batch: 3 more events, total=6 > limit=5, should fail
	result = svc.AreEventsAllowed(ctx, "account-1", 3)
	assert.False(t, result)
}

func TestAreEventsAllowed_TTLExpiry_Resets(t *testing.T) {
	mr, svc := setupRateLimiterTest(t, 5, 1)
	ctx := context.Background()

	// Exhaust the limit
	for i := 0; i < 5; i++ {
		svc.IsAllowed(ctx, "account-1")
	}
	assert.False(t, svc.IsAllowed(ctx, "account-1"))

	// Fast-forward past the 1-minute window
	mr.FastForward(2 * time.Minute)

	// Should be allowed again
	result := svc.IsAllowed(ctx, "account-1")
	assert.True(t, result)
}

func TestAreEventsAllowed_DifferentAccounts_IndependentLimits(t *testing.T) {
	_, svc := setupRateLimiterTest(t, 5, 1)
	ctx := context.Background()

	// Exhaust account-1's limit
	for i := 0; i < 5; i++ {
		svc.IsAllowed(ctx, "account-1")
	}
	assert.False(t, svc.IsAllowed(ctx, "account-1"))

	// account-2 should still be allowed
	result := svc.IsAllowed(ctx, "account-2")
	assert.True(t, result)
}

func TestIsAllowed_RedisDown_ReturnsTrue(t *testing.T) {
	mr, svc := setupRateLimiterTest(t, 5, 1)
	ctx := context.Background()

	// Close Redis to simulate failure
	mr.Close()

	// Fail-open: returns true when Redis is unavailable
	result := svc.IsAllowed(ctx, "account-1")
	assert.True(t, result)
}
