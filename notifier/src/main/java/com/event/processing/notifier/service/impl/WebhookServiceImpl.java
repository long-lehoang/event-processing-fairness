package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.client.WebhookClient;
import com.event.processing.notifier.domain.dto.*;
import com.event.processing.notifier.domain.repository.SegmentRepository;
import com.event.processing.notifier.domain.repository.SubscriberCreatedEventRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
  private final SubscriberCreatedEventRepository subscriberCreatedEventRepository;
  private final SegmentRepository segmentRepository;
  @Value("${spring.kafka.topic.dead-letter-queue-topic.name:webhook-event-dead-letter-queue}")
  private String deadLetterQueueTopic;

  /**
   * Processes the webhook with retry and circuit breaker mechanisms.
   */
  @Retry(name = "webhookRetry", fallbackMethod = "handleFailure")
  @CircuitBreaker(name = "webhookCircuitBreaker", fallbackMethod = "handleCircuitBreak")
  public void processWithRetry(String eventId, WebhookEventDTO eventPayload, String webhookUrl, SubscriberEventDTO webhookPayload) {
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
   * Retrieves event payloads for multiple event IDs.
   */
  @Override
  public Map<String, SubscriberEventDTO> getPayloads(Set<String> eventIds) {
    //TODO: update to support multi event type
    List<SubscriberEventDTO> events = subscriberCreatedEventRepository.fetchEventsWithoutSegments(eventIds);
    Map<String, Set<SegmentDTO>> segmentMap = fetchSegmentsForSubscribers(events);

    // Assign segments
    events.forEach(event -> event.getSubscriber()
        .setSegments(segmentMap.getOrDefault(event.getSubscriber().getId(), Collections.emptySet())));

    return events.stream()
        .collect(Collectors.toMap(SubscriberEventDTO::getId, dto -> dto, (existing, replacement) -> existing));
  }

  /**
   * Retrieves webhook URLs for multiple event IDs.
   */
  @Override
  public Map<String, String> getWebhookUrls(Set<String> eventIds) {
    //TODO: update to support multi event type
    return subscriberCreatedEventRepository.findPostUrlsByEventIds(eventIds)
        .stream()
        .collect(Collectors.toMap(
            SubscriberPostUrlDTO::eventId,
            SubscriberPostUrlDTO::postUrl,
            (existing, replacement) -> existing // Handles duplicate keys gracefully
        ));
  }

  /**
   * Fallback method when all retries are exhausted.
   */
  private void handleFailure(String eventId, WebhookEventDTO eventPayload, String webhookUrl, SubscriberEventDTO webhookPayload, Exception e) {
    log.warn("All retries exhausted. Webhook failed for event: {}, url: {}", eventId, webhookUrl, e);
  }

  /**
   * Fallback method when circuit breaker is triggered.
   */
  private void handleCircuitBreak(String eventId, WebhookEventDTO eventPayload, String webhookUrl, SubscriberEventDTO webhookPayload, Exception e) {
    log.error("Circuit breaker open. Moving event {} to DLQ.", eventId, e);
    meterRegistry.counter(CIRCUIT_BREAKER_OPEN_COUNT).increment();
    deadLetterQueueProducer.publish(deadLetterQueueTopic, eventId, eventPayload);
  }

  /**
   * Fetches segments for a list of subscribers.
   */
  private Map<String, Set<SegmentDTO>> fetchSegmentsForSubscribers(List<SubscriberEventDTO> events) {
    Set<String> subscriberIds = events.stream()
        .map(e -> e.getSubscriber().getId())
        .collect(Collectors.toSet());

    if (subscriberIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return segmentRepository.fetchSegments(subscriberIds).stream()
        .collect(Collectors.groupingBy(SegmentDTO::getSubscriberId, Collectors.toSet()));
  }
}
