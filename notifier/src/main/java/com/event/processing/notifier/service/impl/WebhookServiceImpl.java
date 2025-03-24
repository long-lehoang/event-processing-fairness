package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.client.WebhookClient;
import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import com.event.processing.notifier.producer.DeadLetterQueueProducer;
import com.event.processing.notifier.service.WebhookService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of WebhookService that provides robust webhook delivery with
 * retry,
 * circuit breaker, and monitoring capabilities.
 *
 * Key features:
 * - Retry mechanism for failed webhook deliveries
 * - Circuit breaker for fault tolerance
 * - Performance monitoring with metrics
 * - Dead letter queue for failed events
 * - Detailed logging
 *
 * @author LongLe
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

  private static final String WEBHOOK_EXECUTION_TIME = "webhook.execution.time";
  private static final String WEBHOOK_FAILURE_COUNT = "webhook.failure";
  private static final String CIRCUIT_BREAKER_OPEN_COUNT = "webhook.circuit.open";
  private final WebhookClient webhookClient;
  private final DeadLetterQueueProducer deadLetterQueueProducer;
  private final MeterRegistry meterRegistry;
  @Value("${spring.kafka.topic.dead-letter-queue-topic.name:webhook-event-dead-letter-queue}")
  private String deadLetterQueueTopic;

  /**
   * Processes a webhook notification with retry and circuit breaker mechanisms.
   * This method implements a robust webhook delivery system with:
   * - Automatic retries for transient failures
   * - Circuit breaker for fault tolerance
   * - Performance monitoring
   * - Detailed logging
   *
   * @param eventId        The unique identifier of the event
   * @param eventPayload   The webhook event payload
   * @param webhookUrl     The destination URL for the webhook
   * @param webhookPayload The payload to be sent in the webhook
   */
  @Retry(name = "webhookRetry", fallbackMethod = "handleFailure")
  @CircuitBreaker(name = "webhookCircuitBreaker", fallbackMethod = "handleCircuitBreak")
  public void processWithRetry(String eventId, WebhookEventDTO eventPayload, String webhookUrl,
      BaseEventDTO webhookPayload) {
    Timer.Sample timer = Timer.start(meterRegistry);
    try {
      log.info("Sending webhook for event: {}", eventId);
      boolean success = webhookClient.sendWebhook(webhookUrl, webhookPayload);

      if (!success) {
        throw new RuntimeException("Webhook response failed for event: " + eventId);
      }
      log.info("Webhook successfully processed for event: {}", eventId);
    } catch (Exception e) {
      log.error("Error processing webhook for event {}: {}", eventId, e.getMessage(), e);
      meterRegistry.counter(WEBHOOK_FAILURE_COUNT).increment();
      throw e;
    } finally {
      timer.stop(meterRegistry.timer(WEBHOOK_EXECUTION_TIME));
    }
  }

  /**
   * Fallback method when all retry attempts are exhausted.
   * Logs the failure and allows the application to continue processing other
   * events.
   *
   * @param eventId        The unique identifier of the failed event
   * @param eventPayload   The webhook event payload
   * @param webhookUrl     The destination URL that failed
   * @param webhookPayload The payload that failed to send
   * @param e              The exception that caused the failure
   */
  private void handleFailure(String eventId, WebhookEventDTO eventPayload, String webhookUrl,
      BaseEventDTO webhookPayload, Exception e) {
    log.warn("All retries exhausted. Webhook failed for event: {}, url: {}", eventId, webhookUrl, e);
  }

  /**
   * Fallback method when the circuit breaker is triggered.
   * Moves the failed event to the dead letter queue for later processing
   * and increments the circuit breaker failure counter.
   *
   * @param eventId        The unique identifier of the event
   * @param eventPayload   The webhook event payload
   * @param webhookUrl     The destination URL that failed
   * @param webhookPayload The payload that failed to send
   * @param e              The exception that triggered the circuit breaker
   */
  private void handleCircuitBreak(String eventId, WebhookEventDTO eventPayload, String webhookUrl,
      BaseEventDTO webhookPayload, Exception e) {
    log.error("Circuit breaker open. Moving event {} to DLQ.", eventId, e);
    meterRegistry.counter(CIRCUIT_BREAKER_OPEN_COUNT).increment();
    deadLetterQueueProducer.publish(deadLetterQueueTopic, eventId, eventPayload);
  }
}
