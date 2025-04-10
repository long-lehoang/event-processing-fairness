package com.event.processing.dlq_service.controller;

import com.event.processing.dlq_service.constants.EventStatusConstants;
import com.event.processing.dlq_service.constants.MetricConstants;
import com.event.processing.dlq_service.domain.dto.WebhookEventDTO;
import com.event.processing.dlq_service.service.DeadLetterQueueEventProcessor;
import com.event.processing.dlq_service.service.RetryService;
import com.event.processing.dlq_service.service.WebhookEventService;
import com.event.processing.dlq_service.util.ControllerUtils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Controller for webhook event operations.
 * Provides endpoints for publishing webhook events.
 * Following the Single Responsibility Principle, this controller only handles
 * HTTP requests related to webhook events.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook-events")
public class WebhookEventController {

  private final WebhookEventService webhookEventService;
  private final DeadLetterQueueEventProcessor deadLetterQueueProcessor;
  private final RetryService retryService;
  private final MeterRegistry meterRegistry;
  private final Executor retryTaskExecutor;

  /**
   * Constructor for WebhookEventController.
   *
   * @param webhookEventService      The service for webhook events
   * @param deadLetterQueueProcessor The processor for dead letter queue events
   * @param retryService             The service for retrying events
   * @param meterRegistry            The meter registry for metrics
   * @param retryTaskExecutor        The executor for retry tasks
   */
  public WebhookEventController(
      WebhookEventService webhookEventService,
      DeadLetterQueueEventProcessor deadLetterQueueProcessor,
      RetryService retryService,
      MeterRegistry meterRegistry,
      @Qualifier("retryTaskExecutor") Executor retryTaskExecutor) {
    this.webhookEventService = webhookEventService;
    this.deadLetterQueueProcessor = deadLetterQueueProcessor;
    this.retryService = retryService;
    this.meterRegistry = meterRegistry;
    this.retryTaskExecutor = retryTaskExecutor;
  }

  /**
   * Publishes a webhook event to Kafka.
   *
   * @param event The webhook event to publish
   * @return A response indicating success or failure
   */
  @PostMapping("/publish")
  public ResponseEntity<String> publishEvent(@RequestBody WebhookEventDTO event) {
    log.info("Received request to publish webhook event: {}", event.getEventId());

    return ControllerUtils.executeWithErrorHandling(
        () -> {
          webhookEventService.publishEvent(event);
          return "Event published successfully";
        },
        MetricConstants.API_WEBHOOK_EVENTS_PUBLISHED,
        MetricConstants.API_WEBHOOK_EVENTS_PUBLISH_FAILED,
        "Error publishing webhook event: " + event.getEventId(),
        () -> "Failed to publish event",
        meterRegistry
    );
  }

  /**
   * Creates and publishes a new webhook event.
   *
   * @param eventType The type of the event
   * @param accountId The account ID associated with the event
   * @return The created webhook event
   */
  @PostMapping("/create")
  public ResponseEntity<WebhookEventDTO> createEvent(
      @RequestParam String eventType,
      @RequestParam String accountId) {
    log.info("Received request to create webhook event. Type: {}, Account: {}", eventType, accountId);

    return ControllerUtils.executeWithErrorHandling(
        () -> webhookEventService.createAndPublishEvent(eventType, accountId),
        MetricConstants.API_WEBHOOK_EVENTS_CREATED,
        MetricConstants.API_WEBHOOK_EVENTS_CREATE_FAILED,
        "Error creating webhook event for type: " + eventType + ", account: " + accountId,
        meterRegistry
    );
  }

  /**
   * Retries failed events from the dead letter queue.
   *
   * @param status      The status of events to retry (optional, defaults to "PENDING")
   * @param concurrency The number of threads to use for processing (optional, defaults to 4)
   * @return A list of event IDs that were retried
   */
  @PostMapping("/retry")
  public ResponseEntity<List<String>> retryEvents(
      @RequestParam(required = false, defaultValue = EventStatusConstants.PENDING) String status,
      @RequestParam(required = false, defaultValue = "4") int concurrency) {
    log.info("Received request to retry events with status: {} using {} threads", status, concurrency);

    return ControllerUtils.executeWithErrorHandling(
        () -> {
          List<String> retriedEventIds = retryService.processRetriesConcurrently(status, Instant.now(), concurrency);
          meterRegistry.counter(MetricConstants.API_WEBHOOK_EVENTS_RETRIED).increment(retriedEventIds.size());
          return retriedEventIds;
        },
        MetricConstants.API_WEBHOOK_EVENTS_RETRIED,
        MetricConstants.API_WEBHOOK_EVENTS_RETRY_FAILED,
        "Error retrying events with status: " + status,
        meterRegistry
    );
  }

  /**
   * Retries failed events from the dead letter queue with pagination support.
   * This endpoint is designed for handling large tables efficiently.
   *
   * @param status      The status of events to retry (optional, defaults to "PENDING")
   * @param concurrency The number of threads to use for processing (optional, defaults to 4)
   * @param batchSize   The number of events to process in each batch (optional, defaults to 100)
   * @return A response indicating success or failure
   */
  @PostMapping("/retry/batch")
  public ResponseEntity<String> retryEventsBatch(
      @RequestParam(required = false, defaultValue = EventStatusConstants.PENDING) String status,
      @RequestParam(required = false, defaultValue = "4") int concurrency,
      @RequestParam(required = false, defaultValue = "100") int batchSize) {
    log.info("Received request to retry events in batches with status: {}, concurrency: {}, batchSize: {}",
        status, concurrency, batchSize);

    try {
      // Use CompletableFuture with the dedicated retry executor to process retries asynchronously
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          List<String> retriedEventIds = retryService.processRetriesConcurrently(status, Instant.now(), concurrency, batchSize);
          log.info("Completed batch retry of {} events", retriedEventIds.size());
          meterRegistry.counter(MetricConstants.API_WEBHOOK_EVENTS_RETRIED).increment(retriedEventIds.size());
        } catch (Exception e) {
          log.error("Error in background retry processing", e);
          meterRegistry.counter(MetricConstants.API_WEBHOOK_EVENTS_RETRY_FAILED).increment();
        }
      }, retryTaskExecutor).exceptionally(ex -> {
        log.error("Unhandled exception in background retry task", ex);
        meterRegistry.counter(MetricConstants.API_WEBHOOK_EVENTS_RETRY_FAILED).increment();
        return null;
      });

      // Add a timeout to prevent hanging futures
      future.orTimeout(30, TimeUnit.MINUTES);

      return ResponseEntity.accepted().body("Batch retry process started in the background");
    } catch (Exception e) {
      log.error("Error starting batch retry process", e);
      meterRegistry.counter(MetricConstants.API_WEBHOOK_EVENTS_RETRY_FAILED).increment();
      return ResponseEntity.internalServerError().body("Failed to start batch retry: " + e.getMessage());
    }
  }

  /**
   * Publishes a webhook event to a specific topic.
   *
   * @param topic The topic to publish to
   * @param event The webhook event to publish
   * @return A response indicating success or failure
   */
  @PostMapping("/publish/{topic}")
  public ResponseEntity<String> publishEventToTopic(
      @PathVariable String topic,
      @RequestBody WebhookEventDTO event) {
    log.info("Received request to publish webhook event: {} to topic: {}", event.getEventId(), topic);

    return ControllerUtils.executeWithErrorHandling(
        () -> {
          webhookEventService.publishEvent(topic, event);
          return "Event published successfully to topic: " + topic;
        },
        MetricConstants.API_WEBHOOK_EVENTS_PUBLISHED_TO_TOPIC,
        MetricConstants.API_WEBHOOK_EVENTS_PUBLISH_TO_TOPIC_FAILED,
        "Error publishing webhook event: " + event.getEventId() + " to topic: " + topic,
        () -> "Failed to publish event to topic: " + topic,
        meterRegistry
    );
  }
}
