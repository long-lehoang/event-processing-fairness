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

func setupDeduplicationTest(t *testing.T) (*miniredis.Miniredis, *DeduplicationService) {
	mr, err := miniredis.Run()
	require.NoError(t, err)
	t.Cleanup(func() { mr.Close() })

	client := goredis.NewClient(&goredis.Options{
		Addr: mr.Addr(),
	})
	svc := &DeduplicationService{client: client}
	return mr, svc
}

func TestIsDuplicate_EventNotProcessed_ReturnsFalse(t *testing.T) {
	_, svc := setupDeduplicationTest(t)
	ctx := context.Background()

	result := svc.IsDuplicate(ctx, "event-1")

	assert.False(t, result)
}

func TestIsDuplicate_EventAlreadyProcessed_ReturnsTrue(t *testing.T) {
	mr, svc := setupDeduplicationTest(t)
	ctx := context.Background()

	// Pre-set the key in Redis
	mr.Set("deduplicate:event:event-1", "1")

	result := svc.IsDuplicate(ctx, "event-1")

	assert.True(t, result)
}

func TestMarkProcessed_SetsKeyWithExpiry(t *testing.T) {
	mr, svc := setupDeduplicationTest(t)
	ctx := context.Background()

	err := svc.MarkProcessed(ctx, "event-1")

	require.NoError(t, err)

	// Verify key exists with value "1"
	val, err := mr.Get("deduplicate:event:event-1")
	require.NoError(t, err)
	assert.Equal(t, "1", val)

	// Verify TTL is set (24 hours)
	assert.True(t, mr.TTL("deduplicate:event:event-1") > 0)
}

func TestMarkProcessed_ThenIsDuplicate_ReturnsTrue(t *testing.T) {
	_, svc := setupDeduplicationTest(t)
	ctx := context.Background()

	err := svc.MarkProcessed(ctx, "event-1")
	require.NoError(t, err)

	result := svc.IsDuplicate(ctx, "event-1")

	assert.True(t, result)
}

func TestIsDuplicate_AfterExpiry_ReturnsFalse(t *testing.T) {
	mr, svc := setupDeduplicationTest(t)
	ctx := context.Background()

	err := svc.MarkProcessed(ctx, "event-1")
	require.NoError(t, err)

	// Fast-forward past the 24h TTL
	mr.FastForward(25 * time.Hour)

	result := svc.IsDuplicate(ctx, "event-1")

	assert.False(t, result)
}

func TestIsDuplicate_RedisDown_ReturnsFalse(t *testing.T) {
	mr, svc := setupDeduplicationTest(t)
	ctx := context.Background()

	// Close Redis to simulate failure
	mr.Close()

	result := svc.IsDuplicate(ctx, "event-1")

	// Fail-open: returns false when Redis is unavailable
	assert.False(t, result)
}
