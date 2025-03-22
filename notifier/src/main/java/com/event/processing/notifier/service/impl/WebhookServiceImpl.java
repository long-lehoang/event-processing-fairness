package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.client.WebhookClient;
import com.event.processing.notifier.domain.dto.SubscriberEventDTO;
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

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {
  private final WebhookClient webhookClient;
  private final DeadLetterQueueProducer deadLetterQueueProducer;
  private final MeterRegistry meterRegistry;
  private static final String WEBHOOK_METRIC = "webhook.execution.time";

  @Value("${spring.kafka.topic.dead-letter-queue-topic:webhook-event-dead-letter-queue}")
  private String deadLetterQueueTopic;
  private SubscriberEventDTO getPayload(String eventId) {
    //TODO: get payload from db
    return SubscriberEventDTO.builder().build();
  }

  @Retry(name = "webhookRetry", fallbackMethod = "handleFailure")
  @CircuitBreaker(name = "webhookCircuitBreaker", fallbackMethod = "handleCircuitBreak")
  public void processWithRetry(String eventId, String webhookUrl, SubscriberEventDTO webhookPayload) {
    Timer.Sample timer = Timer.start(meterRegistry);

    try {
      log.info("Calling webhook for event: {}", eventId);
      boolean success = webhookClient.sendWebhook(webhookUrl, webhookPayload);

      if (!success) {
        throw new RuntimeException("Webhook response was not successful for event: " + eventId);
      }
    } catch (Exception e) {
      meterRegistry.counter("webhook.failure").increment();
      throw e;
    } finally {
      timer.stop(meterRegistry.timer(WEBHOOK_METRIC));
    }

  }

  @Override
  public Map<String, SubscriberEventDTO> getPayloads(Set<String> eventIds) {
    //TODO: fetch payload
    return null;
  }

  @Override
  public Map<String, String> getWebhookUrls(Set<String> eventIds) {
    //TODO: fetch urls
    return null;
  }

  private void handleFailure(String eventId, Exception e) {
    log.warn("Retry exhausted, webhook failed: {}", eventId, e);
  }

  private void handleCircuitBreak(String eventId, WebhookEventDTO payload, Exception e) {
    log.error("Circuit breaker opened for event {}. Moving to DLQ", eventId, e);
    meterRegistry.counter("webhook.circuit.open").increment();
    deadLetterQueueProducer.publish(deadLetterQueueTopic, eventId, payload);
  }
}
