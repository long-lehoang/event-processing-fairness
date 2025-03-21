package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.client.WebhookClient;
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
  private String getPayload(String eventId) {
    //TODO: get payload from db
    return "";
  }

  @Retry(name = "webhookRetry", fallbackMethod = "handleFailure")
  @CircuitBreaker(name = "webhookCircuitBreaker", fallbackMethod = "handleCircuitBreak")
  public void processWithRetry(String eventId, String payload) {
    Timer.Sample timer = Timer.start(meterRegistry);

    try {
      // Fetch payload from DB
      String fullPayload = this.getPayload(eventId);
      if (fullPayload == null) {
        log.warn("No payload found for event: {}", eventId);
        return;
      }

      log.info("Calling webhook for event: {}", eventId);
      webhookClient.sendWebhook(payload);
    } catch (Exception e) {
      meterRegistry.counter("webhook.failure").increment();
      throw e;
    } finally {
      timer.stop(meterRegistry.timer(WEBHOOK_METRIC));
    }

  }

  private void handleFailure(String eventId, String payload, Exception e) {
    log.warn("Retry exhausted, webhook failed: {}", eventId, e);
    deadLetterQueueProducer.publish(deadLetterQueueTopic, eventId, payload);
  }

  private void handleCircuitBreak(String eventId, String payload, Exception e) {
    log.error("Circuit breaker opened for event {}. Moving to DLQ", eventId, e);
    meterRegistry.counter("webhook.circuit.open").increment();
    deadLetterQueueProducer.publish(deadLetterQueueTopic, eventId, payload);
  }
}
