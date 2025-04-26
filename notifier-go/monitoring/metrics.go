package monitoring

import (
	"time"
	
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

// Metrics holds all the Prometheus metrics for the application
type Metrics struct {
	// Kafka metrics
	kafkaEventCount        prometheus.Counter
	kafkaBatchProcessingTime prometheus.Histogram
	
	// Webhook metrics
	webhookSuccessCount    prometheus.Counter
	webhookFailureCount    prometheus.Counter
	
	// Deduplication metrics
	duplicateEventCount    prometheus.Counter
	
	// Rate limiting metrics
	rateLimitExceededCount prometheus.Counter
}

// NewMetrics creates a new Metrics instance
func NewMetrics() *Metrics {
	return &Metrics{
		kafkaEventCount: promauto.NewCounter(prometheus.CounterOpts{
			Name: "kafka_event_count",
			Help: "The total number of events received from Kafka",
		}),
		kafkaBatchProcessingTime: promauto.NewHistogram(prometheus.HistogramOpts{
			Name: "kafka_batch_processing_time_seconds",
			Help: "The time taken to process a batch of Kafka events",
			Buckets: prometheus.DefBuckets,
		}),
		webhookSuccessCount: promauto.NewCounter(prometheus.CounterOpts{
			Name: "webhook_success_count",
			Help: "The total number of successful webhook deliveries",
		}),
		webhookFailureCount: promauto.NewCounter(prometheus.CounterOpts{
			Name: "webhook_failure_count",
			Help: "The total number of failed webhook deliveries",
		}),
		duplicateEventCount: promauto.NewCounter(prometheus.CounterOpts{
			Name: "duplicate_event_count",
			Help: "The total number of duplicate events detected",
		}),
		rateLimitExceededCount: promauto.NewCounter(prometheus.CounterOpts{
			Name: "rate_limit_exceeded_count",
			Help: "The total number of events that exceeded rate limits",
		}),
	}
}

// IncrementKafkaEventCount increments the Kafka event counter
func (m *Metrics) IncrementKafkaEventCount(count int) {
	m.kafkaEventCount.Add(float64(count))
}

// RecordBatchProcessingTime records the time taken to process a batch of Kafka events
func (m *Metrics) RecordBatchProcessingTime(duration time.Duration) {
	m.kafkaBatchProcessingTime.Observe(duration.Seconds())
}

// IncrementWebhookSuccess increments the webhook success counter
func (m *Metrics) IncrementWebhookSuccess() {
	m.webhookSuccessCount.Inc()
}

// IncrementWebhookFailure increments the webhook failure counter
func (m *Metrics) IncrementWebhookFailure() {
	m.webhookFailureCount.Inc()
}

// IncrementDuplicateEventCount increments the duplicate event counter
func (m *Metrics) IncrementDuplicateEventCount() {
	m.duplicateEventCount.Inc()
}

// IncrementRateLimitExceededCount increments the rate limit exceeded counter
func (m *Metrics) IncrementRateLimitExceededCount() {
	m.rateLimitExceededCount.Inc()
}
