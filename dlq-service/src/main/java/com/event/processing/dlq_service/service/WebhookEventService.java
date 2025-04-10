package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.domain.dto.WebhookEventDTO;
import com.event.processing.dlq_service.producer.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for webhook event operations.
 * This service provides methods for creating and publishing webhook events.
 * Following the Single Responsibility Principle, this class only handles
 * webhook event creation and publishing operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventService {

  private final EventProducer<WebhookEventDTO> webhookEventProducer;

  /**
   * Creates and publishes a new webhook event.
   *
   * @param eventType The type of the event
   * @param accountId The account ID associated with the event
   * @return The created webhook event
   */
  public WebhookEventDTO createAndPublishEvent(String eventType, String accountId) {
    WebhookEventDTO event = WebhookEventDTO.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType(eventType)
        .accountId(accountId)
        .build();

    log.info("Created new webhook event: {}", event.getEventId());
    webhookEventProducer.publishEvent(event);

    return event;
  }

  /**
   * Publishes an existing webhook event.
   *
   * @param event The webhook event to publish
   */
  public void publishEvent(WebhookEventDTO event) {
    log.info("Publishing webhook event: {}", event.getEventId());
    webhookEventProducer.publishEvent(event);
  }

  /**
   * Publishes an existing webhook event to a specific topic.
   *
   * @param topic The topic to publish to
   * @param event The webhook event to publish
   */
  public void publishEvent(String topic, WebhookEventDTO event) {
    log.info("Publishing webhook event: {} to topic: {}", event.getEventId(), topic);
    webhookEventProducer.publishEvent(topic, event);
  }
}
