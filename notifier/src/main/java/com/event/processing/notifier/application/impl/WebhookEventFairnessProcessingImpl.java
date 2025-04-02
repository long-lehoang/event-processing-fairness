package com.event.processing.notifier.application.impl;

import com.event.processing.notifier.application.WebhookEventProcessing;
import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import com.event.processing.notifier.producer.EventProducer;
import com.event.processing.notifier.service.DeduplicationService;
import com.event.processing.notifier.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of WebhookEventProcessing that provides fairness-aware event
 * processing.
 * This class ensures fair processing of webhook events by implementing:
 * - Deduplication to prevent duplicate processing
 * - Event processing with delivery retries
 * <p>
 * Key features:
 * - Duplicate event detection and handling
 * - Comprehensive logging and error handling
 *
 * @author LongLe
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventFairnessProcessingImpl implements WebhookEventProcessing {

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

  /**
   * Service for detecting and handling duplicate events.
   */
  private final DeduplicationService deduplicationService;

  /**
   * Service for handling webhook delivery and retries.
   */
  private final WebhookService webhookService;

  /**
   * Producer for republishing events.
   */
  private final EventProducer eventProducer;

  /**
   * Kafka topic name for webhook events.
   * Configurable via application properties.
   */
  @Value("${spring.kafka.topic.webhook-event.name:webhook-events}")
  private String webhookEventTopic;

  /**
   * Processes a webhook event with fairness guarantees.
   * This method:
   * - Checks for duplicate events
   * - Handles webhook delivery with retries
   * - Manages event state and logging
   *
   * @param eventId        The unique identifier of the event
   * @param eventPayload   The webhook event payload
   * @param url            The destination URL for the webhook
   * @param webhookPayload The transformed payload to be sent
   */
  @Override
  public void process(String eventId, WebhookEventDTO eventPayload, String url, BaseEventDTO webhookPayload) {
    log.info("Processing event: {}", eventId);

    if (isDuplicate(eventId))
      return;

    try {
      webhookService.processWithRetry(eventId, eventPayload, url, webhookPayload);
      deduplicationService.markProcessed(eventId);
      log.info("Successfully processed event: {}", eventId);
    } catch (Exception e) {
      log.error("Failed to process event {}: {}", eventId, e.getMessage(), e);
    }
  }

  /**
   * Checks if an event is a duplicate and should be skipped.
   *
   * @param eventId The unique identifier of the event to check
   * @return true if the event is a duplicate, false otherwise
   */
  private boolean isDuplicate(String eventId) {
    if (deduplicationService.isDuplicate(eventId)) {
      log.warn("Skipping duplicate event: {}", eventId);
      return true;
    }
    return false;
  }
}
