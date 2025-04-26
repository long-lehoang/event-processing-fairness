package util

// Prometheus metric constants
const (
	// Kafka metrics
	KafkaEventCount        = "kafka_event_count"
	KafkaBatchProcessingTime = "kafka_batch_processing_time_seconds"
	
	// Webhook metrics
	WebhookSuccessCount    = "webhook_success_count"
	WebhookFailureCount    = "webhook_failure_count"
	
	// Deduplication metrics
	DuplicateEventCount    = "duplicate_event_count"
	
	// Rate limiting metrics
	RateLimitExceededCount = "rate_limit_exceeded_count"
)

// Redis constants
const (
	// Deduplication
	DeduplicationKeyPrefix = "deduplicate:event:"
	DeduplicationExpiry    = 24 // hours
	
	// Rate limiting
	RateLimitKeyPrefix     = "rate:limit:"
)

// Kafka constants
const (
	// Topics
	DefaultWebhookEventTopic = "webhook-events"
	DefaultDLQTopic          = "webhook-event-dead-letter-queue"
	
	// Consumer
	DefaultConsumerGroup     = "event-processing-group"
)

// Error messages
const (
	ErrRateLimitExceeded     = "rate limit exceeded"
	ErrDuplicateEvent        = "duplicate event"
	ErrWebhookDeliveryFailed = "webhook delivery failed"
)
