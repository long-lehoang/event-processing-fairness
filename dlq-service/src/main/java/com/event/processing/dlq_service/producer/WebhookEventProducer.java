package com.event.processing.dlq_service.producer;

import com.event.processing.dlq_service.constants.MetricConstants;
import com.event.processing.dlq_service.domain.dto.WebhookEventDTO;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of EventProducer for webhook events.
 * This class is responsible for publishing webhook events to Kafka.
 * Following the Single Responsibility Principle, this class only handles
 * the publishing of webhook events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventProducer implements EventProducer<WebhookEventDTO> {

  private final KafkaTemplate<String, WebhookEventDTO> kafkaTemplate;
  private final MeterRegistry meterRegistry;

  @Value("${spring.kafka.topic.webhook-events}")
  private String webhookEventsTopic;

  /**
   * Publishes a webhook event to the configured default topic.
   *
   * @param event The webhook event to publish
   * @return A CompletableFuture that will be completed when the send operation completes
   */
  @Override
  public CompletableFuture<SendResult<String, WebhookEventDTO>> publishEvent(WebhookEventDTO event) {
    return publishEvent(webhookEventsTopic, event);
  }

  /**
   * Publishes a webhook event to a specific topic.
   *
   * @param topic The topic to publish to
   * @param event The webhook event to publish
   * @return A CompletableFuture that will be completed when the send operation completes
   */
  @Override
  public CompletableFuture<SendResult<String, WebhookEventDTO>> publishEvent(String topic, WebhookEventDTO event) {
    log.info("Publishing webhook event: {} to topic: {}", event.getEventId(), topic);

    CompletableFuture<SendResult<String, WebhookEventDTO>> future = kafkaTemplate.send(topic, event.getEventId(), event);

    future.whenComplete((result, ex) -> {
      if (ex == null) {
        log.info("Successfully published webhook event: {} to topic: {}", event.getEventId(), topic);
        meterRegistry.counter(MetricConstants.WEBHOOK_EVENTS_PUBLISHED).increment();
      } else {
        log.error("Failed to publish webhook event: {} to topic: {}", event.getEventId(), topic, ex);
        meterRegistry.counter(MetricConstants.WEBHOOK_EVENTS_PUBLISH_FAILED).increment();
      }
    });

    return future;
  }
}
