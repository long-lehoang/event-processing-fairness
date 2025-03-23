package com.event.processing.notifier.consumer;

import com.event.processing.notifier.application.WebhookEventProcessing;
import com.event.processing.notifier.domain.dto.SubscriberEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import com.event.processing.notifier.service.WebhookService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventKafkaConsumer implements EventConsumer {

  private final WebhookEventProcessing eventProcessingService;
  private final WebhookService webhookService;
  private final MeterRegistry meterRegistry;
  private final ThreadPoolTaskExecutor kafkaConsumerExecutor;

  @Value("${spring.kafka.topic.webhook-event.name:webhook-events}")
  private String topic;

  @KafkaListener(
      topics = "${spring.kafka.topic.webhook-event.name:webhook-events}",
      groupId = "webhook-group",
      containerFactory = "kafkaListenerContainerFactory"
  )
  @Override
  public void consume(List<ConsumerRecord<String, WebhookEventDTO>> records, Acknowledgment acknowledgment) {
    if (records.isEmpty()) {
      log.warn("No events received, skipping processing.");
      return;
    }

    log.info("Received {} events", records.size());

    Set<String> eventIds = extractEventIds(records);

    Map<String, SubscriberEventDTO> payloadMap;
    Map<String, String> webhookUrlMap;
    try {
      payloadMap = webhookService.getPayloads(eventIds);
      webhookUrlMap = webhookService.getWebhookUrls(eventIds);
    } catch (Exception e) {
      log.error("Failed to fetch event data from DB", e);
      return;
    }

    processBatch(records, acknowledgment, webhookUrlMap, payloadMap);
  }

  private Set<String> extractEventIds(List<ConsumerRecord<String, WebhookEventDTO>> records) {
    return records.stream()
        .map(ConsumerRecord::key)
        .collect(Collectors.toSet());
  }

  private void processBatch(
      List<ConsumerRecord<String, WebhookEventDTO>> records,
      Acknowledgment acknowledgment,
      Map<String, String> webhookUrlMap,
      Map<String, SubscriberEventDTO> payloadMap
  ) {
    Timer batchProcessingTimer = meterRegistry.timer("kafka.batch.processing.time");
    batchProcessingTimer.record(() -> {
      List<CompletableFuture<Void>> futures = records.stream()
          .map(event -> CompletableFuture.supplyAsync(() -> processEvent(event, webhookUrlMap, payloadMap), kafkaConsumerExecutor)
              .exceptionally(ex -> handleProcessingFailure(event.key(), ex)))
          .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .thenRun(() -> acknowledge(acknowledgment, records.size()))
          .join();
    });
  }

  private Void handleProcessingFailure(String eventId, Throwable ex) {
    log.error("Processing failed for event {}: {}", eventId, ex.getMessage());
    return null;
  }

  private void acknowledge(Acknowledgment acknowledgment, int recordCount) {
    try {
      acknowledgment.acknowledge();
      log.info("Acknowledged {} events", recordCount);
    } catch (Exception e) {
      log.error("Failed to acknowledge Kafka offsets", e);
    }
  }

  private Void processEvent(
      ConsumerRecord<String, WebhookEventDTO> event,
      Map<String, String> webhookUrlMap,
      Map<String, SubscriberEventDTO> payloadMap
  ) {
    String eventId = event.key();
    WebhookEventDTO eventPayload = event.value();

    if (eventPayload == null) {
      log.warn("Skipping event {} due to missing payload", eventId);
      return null;
    }

    String url = webhookUrlMap.get(eventId);
    SubscriberEventDTO webhookPayload = payloadMap.get(eventId);

    if (url == null || webhookPayload == null) {
      log.warn("Skipping event {} due to missing webhook data (URL: {}, Payload: {})", eventId, url, webhookPayload);
      return null;
    }

    Timer eventProcessingTimer = meterRegistry.timer("kafka.event.processing.time", "eventId", eventId);
    eventProcessingTimer.record(() -> {
      try {
        eventProcessingService.process(eventId, eventPayload, url, webhookPayload);
        log.debug("Successfully processed event {}", eventId);
      } catch (Exception e) {
        log.error("Failed to process event {}", eventId, e);
      }
    });

    return null;
  }
}
