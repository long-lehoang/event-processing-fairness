package com.event.processing.notifier.consumer;

import com.event.processing.notifier.application.WebhookEventProcessing;
import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import com.event.processing.notifier.domain.service.WebhookEventService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.event.processing.notifier.util.PromeTheusMetricContants.*;

/**
 * Kafka-based implementation of the EventConsumer interface for processing
 * webhook events.
 * This class handles the consumption and processing of webhook events from
 * Kafka topics,
 * with support for batch processing, parallel execution, and monitoring.
 * <p>
 * Key features:
 * - Batch processing of webhook events
 * - Parallel processing using thread pool
 * - Event grouping by type
 * - Performance monitoring with metrics
 * - Error handling and logging
 * - Manual acknowledgment support
 *
 * @author LongLe
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventKafkaConsumer implements EventConsumer {

  // Dependency Injection
  private final WebhookEventProcessing eventProcessingService;
  private final WebhookEventService webhookEventService;
  private final MeterRegistry meterRegistry;
  private final ThreadPoolTaskExecutor kafkaConsumerExecutor;

  @Value("${spring.kafka.topic.webhook-event.name:webhook-events}")
  private String topic;

  /**
   * Consumes webhook events from Kafka topic and processes them in batches.
   * Events are grouped by type and processed in parallel using a thread pool.
   * Processing includes fetching webhook URLs and payloads, and sending
   * notifications.
   *
   * @param records        List of Kafka consumer records containing webhook
   *                       events
   * @param acknowledgment Acknowledgment object to mark successful processing
   */
  @KafkaListener(topics = "${spring.kafka.topic.webhook-event.name:webhook-events}", groupId = "webhook-group", containerFactory = "kafkaListenerContainerFactory")
  @Override
  public void consume(List<ConsumerRecord<String, WebhookEventDTO>> records, Acknowledgment acknowledgment) {
    Timer batchProcessingTimer = meterRegistry.timer(METRIC_KAFKA_BATCH_PROCESSING_TIME);
    batchProcessingTimer.record(() -> {
      if (records.isEmpty()) {
        log.warn("No events received, skipping processing.");
        return;
      }

      log.info("Received {} events", records.size());

      Map<String, List<ConsumerRecord<String, WebhookEventDTO>>> eventsByType = groupEventsByType(records);

      List<CompletableFuture<Void>> futures = eventsByType.entrySet().stream()
          .map(entry -> CompletableFuture.runAsync(() -> processEventGroup(entry.getKey(), entry.getValue()),
              kafkaConsumerExecutor))
          .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .thenRun(() -> acknowledge(acknowledgment, records.size()))
          .join(); // Ensure we wait before acknowledging
    });
  }

  /**
   * Groups consumer records by their event type for batch processing.
   *
   * @param records List of consumer records to group
   * @return Map of event types to their corresponding consumer records
   */
  private Map<String, List<ConsumerRecord<String, WebhookEventDTO>>> groupEventsByType(
      List<ConsumerRecord<String, WebhookEventDTO>> records) {
    return records.stream()
        .collect(Collectors.groupingBy(record -> record.value().getEventType()));
  }

  /**
   * Processes a group of events of the same type.
   * Fetches webhook URLs and payloads for the events and initiates batch
   * processing.
   *
   * @param eventType  The type of events being processed
   * @param eventGroup List of consumer records for the event type
   */
  private void processEventGroup(String eventType, List<ConsumerRecord<String, WebhookEventDTO>> eventGroup) {
    Set<String> eventIds = eventGroup.stream()
        .map(record -> record.value().getEventId())
        .collect(Collectors.toSet());

    Map<String, BaseEventDTO> payloadMap;
    Map<String, String> webhookUrlMap;
    try {
      payloadMap = webhookEventService.getPayloads(eventType, eventIds);
      webhookUrlMap = webhookEventService.getWebhookUrls(eventType, eventIds);
    } catch (Exception e) {
      log.error("Failed to fetch event data from DB for event type: {}", eventType, e);
      return;
    }

    processBatch(eventGroup, webhookUrlMap, payloadMap);
  }

  /**
   * Processes a batch of events asynchronously using the thread pool.
   * Each event is processed in parallel with performance monitoring.
   *
   * @param records       List of consumer records to process
   * @param webhookUrlMap Map of event IDs to their webhook URLs
   * @param payloadMap    Map of event IDs to their payloads
   */
  private void processBatch(
      List<ConsumerRecord<String, WebhookEventDTO>> records,
      Map<String, String> webhookUrlMap,
      Map<String, BaseEventDTO> payloadMap) {

    List<CompletableFuture<Void>> eventFutures = records.stream()
        .map(event -> CompletableFuture.supplyAsync(
                () -> processSingleEvent(event, webhookUrlMap, payloadMap), kafkaConsumerExecutor)
            .exceptionally(ex -> handleProcessingFailure(event.key(), ex)))
        .toList();

    // Ensure all events are processed before moving forward
    CompletableFuture.allOf(eventFutures.toArray(new CompletableFuture[0])).join();
  }

  /**
   * Handles processing failures for individual events.
   * Logs the error and returns null to allow batch processing to continue.
   *
   * @param eventId The ID of the event that failed processing
   * @param ex      The exception that occurred during processing
   * @return null to allow batch processing to continue
   */
  private Void handleProcessingFailure(String eventId, Throwable ex) {
    log.error("Processing failed for event {}: {}", eventId, ex.getMessage());
    return null;
  }

  /**
   * Acknowledges the successful processing of a batch of records.
   * Logs success or failure of the acknowledgment process.
   *
   * @param acknowledgment The acknowledgment object to use
   * @param recordCount    The number of records being acknowledged
   */
  private void acknowledge(Acknowledgment acknowledgment, int recordCount) {
    try {
      acknowledgment.acknowledge();
      log.info("Acknowledged {} events", recordCount);
    } catch (Exception e) {
      log.error("Failed to acknowledge Kafka offsets", e);
    }
  }

  /**
   * Processes a single webhook event.
   * Validates the event data and sends the webhook notification.
   * Includes performance monitoring for the processing time.
   *
   * @param event         The consumer record containing the event
   * @param webhookUrlMap Map of event IDs to their webhook URLs
   * @param payloadMap    Map of event IDs to their payloads
   * @return null after processing is complete
   */
  private Void processSingleEvent(
      ConsumerRecord<String, WebhookEventDTO> event,
      Map<String, String> webhookUrlMap,
      Map<String, BaseEventDTO> payloadMap) {

    meterRegistry.counter(KAFKA_EVENT_COUNT).increment();

    String eventId = event.key();
    WebhookEventDTO eventPayload = event.value();

    if (eventPayload == null) {
      log.warn("Skipping event {} due to missing payload", eventId);
      return null;
    }

    String url = webhookUrlMap.get(eventId);
    BaseEventDTO webhookPayload = payloadMap.get(eventId);

    if (url == null || webhookPayload == null) {
      log.warn("Skipping event {} due to missing webhook data (URL: {}, Payload: {})", eventId, url, webhookPayload);
      return null;
    }

    try {
      eventProcessingService.process(eventId, eventPayload, url, webhookPayload);
      log.debug("Successfully processed event {}", eventId);
    } catch (Exception e) {
      log.error("Failed to process event {}", eventId, e);
    }

    return null;
  }
}
