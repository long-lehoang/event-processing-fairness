package impl

import (
	"context"
	"fmt"
	"log"
	"time"
	
	"github.com/cenkalti/backoff/v4"
	"github.com/event-processing/notifier-go/config"
	"github.com/event-processing/notifier-go/domain/dto"
	"github.com/event-processing/notifier-go/service"
	"github.com/sony/gobreaker"
)

// WebhookServiceImpl implements the WebhookService interface
type WebhookServiceImpl struct {
	webhookClient service.WebhookClient
	dlqProducer   service.DeadLetterQueueProducer
	metrics       service.MetricsService
	config        *config.Config
	circuitBreakers map[string]*gobreaker.CircuitBreaker
}

// NewWebhookService creates a new WebhookServiceImpl
func NewWebhookService(
	webhookClient service.WebhookClient,
	dlqProducer service.DeadLetterQueueProducer,
	metrics service.MetricsService,
	config *config.Config,
) *WebhookServiceImpl {
	return &WebhookServiceImpl{
		webhookClient: webhookClient,
		dlqProducer:   dlqProducer,
		metrics:       metrics,
		config:        config,
		circuitBreakers: make(map[string]*gobreaker.CircuitBreaker),
	}
}

// ProcessWithRetry processes a webhook notification with retry capabilities
func (s *WebhookServiceImpl) ProcessWithRetry(
	ctx context.Context,
	eventID string,
	eventPayload *dto.WebhookEventDTO,
	url string,
	payload dto.BaseEventDTO,
) error {
	log.Printf("Processing webhook for event_id: %s, account_id: %s", eventID, eventPayload.AccountID)
	
	// Get or create circuit breaker for this URL
	cb := s.getCircuitBreaker(url)
	
	// Create retry backoff
	retryConfig := s.config.Resilience.Retry.WebhookRetry
	exponentialBackoff := backoff.NewExponentialBackOff()
	exponentialBackoff.InitialInterval = retryConfig.WaitDuration
	exponentialBackoff.Multiplier = retryConfig.ExponentialBackoffMultiplier
	exponentialBackoff.MaxElapsedTime = retryConfig.WaitDuration * time.Duration(retryConfig.MaxAttempts)
	
	// Execute with circuit breaker and retry
	var err error
	operation := func() error {
		_, execErr := cb.Execute(func() (interface{}, error) {
			success, err := s.webhookClient.SendWebhook(ctx, url, payload)
			if !success {
				return nil, fmt.Errorf("webhook response failed for event: %s", eventID)
			}
			return nil, err
		})
		
		return execErr
	}
	
	// Execute with retry
	err = backoff.Retry(operation, exponentialBackoff)
	
	if err != nil {
		log.Printf("Error processing webhook for event %s: %v", eventID, err)
		s.metrics.IncrementWebhookFailure()
		
		// Send to DLQ
		dlqEvent := dto.FromWebhookEvent(eventPayload, err.Error(), "Webhook delivery failed after retries")
		dlqTopic := s.config.Kafka.Topics.DeadLetterQueue.Name
		if dlqErr := s.dlqProducer.Publish(ctx, dlqTopic, eventID, dlqEvent); dlqErr != nil {
			log.Printf("Failed to publish to DLQ: %v", dlqErr)
		}
		
		return err
	}
	
	log.Printf("Webhook successfully processed for event: %s", eventID)
	s.metrics.IncrementWebhookSuccess()
	return nil
}

// getCircuitBreaker gets or creates a circuit breaker for the given URL
func (s *WebhookServiceImpl) getCircuitBreaker(url string) *gobreaker.CircuitBreaker {
	if cb, exists := s.circuitBreakers[url]; exists {
		return cb
	}
	
	cbConfig := s.config.Resilience.CircuitBreaker.WebhookCircuitBreaker
	
	settings := gobreaker.Settings{
		Name:        url,
		MaxRequests: uint32(cbConfig.PermittedNumberOfCallsInHalfOpenState),
		Interval:    cbConfig.WaitDurationInOpenState,
		Timeout:     cbConfig.WaitDurationInOpenState,
		ReadyToTrip: func(counts gobreaker.Counts) bool {
			failureRatio := float64(counts.TotalFailures) / float64(counts.Requests)
			return counts.Requests >= uint32(cbConfig.MinimumNumberOfCalls) && failureRatio >= cbConfig.FailureRateThreshold/100.0
		},
		OnStateChange: func(name string, from gobreaker.State, to gobreaker.State) {
			log.Printf("Circuit breaker %s state changed from %s to %s", name, from, to)
		},
	}
	
	cb := gobreaker.NewCircuitBreaker(settings)
	s.circuitBreakers[url] = cb
	return cb
}
