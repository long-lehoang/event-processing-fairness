package com.event.processing.notifier.application.impl;

import com.event.processing.notifier.application.WebhookEventProcessingService;
import com.event.processing.notifier.producer.EventProducer;
import com.event.processing.notifier.service.DeduplicationService;
import com.event.processing.notifier.service.RateLimiterService;
import com.event.processing.notifier.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventFairnessProcessingServiceImpl implements WebhookEventProcessingService {

  private final DeduplicationService deduplicationService;
  private final RateLimiterService rateLimiterService;
  private final WebhookService webhookService; // Use Interface, Not Implementation
  private final EventProducer eventProducer;

  @Value("${spring.kafka.topic.webhook-event:webhook-events}")
  private String webhookEventTopic;

  @Override
  public void process(String eventId, String payload) {
    log.info("Processing event: {}", eventId);

    if (isDuplicate(eventId)) return;
    if (isRateLimited(eventId, payload)) return;

    try {
      webhookService.processWithRetry(eventId, payload);
      deduplicationService.markProcessed(eventId);
      log.info("Successfully processed event: {}", eventId);
    } catch (Exception e) {
      log.error("Failed to process event {}: {}", eventId, e.getMessage(), e);
    }
  }

  private boolean isDuplicate(String eventId) {
    if (deduplicationService.isDuplicate(eventId)) {
      log.warn("Skipping duplicate event: {}", eventId);
      return true;
    }
    return false;
  }

  private boolean isRateLimited(String eventId, String payload) {
    if (rateLimiterService.isExceedLimit(eventId)) {
      log.warn("Event {} is rate limited. Pushing back to queue.", eventId);
      eventProducer.publish(webhookEventTopic, eventId, payload);
      return true;
    }
    return false;
  }
}
