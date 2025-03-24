package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.client.WebhookClient;
import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import com.event.processing.notifier.producer.DeadLetterQueueProducer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookServiceImpl Tests")
class WebhookServiceImplTest {

  @Mock
  private WebhookClient webhookClient;

  @Mock
  private DeadLetterQueueProducer deadLetterQueueProducer;

  @Mock
  private MeterRegistry meterRegistry;

  @Mock
  private CircuitBreakerRegistry circuitBreakerRegistry;

  @Mock
  private RetryRegistry retryRegistry;

  @Mock
  private CircuitBreaker circuitBreaker;

  @Mock
  private Retry retry;

  @Mock
  private Timer.Sample timer;

  private WebhookServiceImpl webhookService;

  private static final String EVENT_ID = "test-event-id";
  private static final String WEBHOOK_URL = "http://test-webhook-url";
  private static final String DEAD_LETTER_QUEUE_TOPIC = "webhook-event-dead-letter-queue";

  @BeforeEach
  void setUp() {
    webhookService = new WebhookServiceImpl(
            webhookClient,
            deadLetterQueueProducer,
            meterRegistry);
  }

  @Nested
  @DisplayName("processWithRetry Tests")
  class ProcessWithRetryTests {

    @Test
    @DisplayName("Should successfully process webhook")
    void shouldSuccessfullyProcessWebhook() {
      // Arrange
      WebhookEventDTO eventPayload = new WebhookEventDTO();
      BaseEventDTO webhookPayload = new BaseEventDTO();
      when(webhookClient.sendWebhook(anyString(), any(BaseEventDTO.class))).thenReturn(true);
      when(meterRegistry.timer(anyString())).thenReturn(mock(Timer.class));
      when(Timer.start(any(MeterRegistry.class))).thenReturn(timer);

      // Act
      webhookService.processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

      // Assert
      verify(webhookClient).sendWebhook(WEBHOOK_URL, webhookPayload);
      verify(timer).stop(any(Timer.class));
    }

    @Test
    @DisplayName("Should handle webhook failure")
    void shouldHandleWebhookFailure() {
      // Arrange
      WebhookEventDTO eventPayload = new WebhookEventDTO();
      BaseEventDTO webhookPayload = new BaseEventDTO();
      when(webhookClient.sendWebhook(anyString(), any(BaseEventDTO.class))).thenReturn(false);
      when(meterRegistry.timer(anyString())).thenReturn(mock(Timer.class));
      when(Timer.start(any(MeterRegistry.class))).thenReturn(timer);

      // Act & Assert
      assertThatThrownBy(
              () -> webhookService.processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload))
              .isInstanceOf(RuntimeException.class)
              .hasMessage("Webhook response failed for event: " + EVENT_ID);
      verify(meterRegistry).counter("webhook.failure");
    }

    @Test
    @DisplayName("Should handle circuit breaker scenario")
    void shouldHandleCircuitBreakerScenario() {
      // Arrange
      WebhookEventDTO eventPayload = new WebhookEventDTO();
      BaseEventDTO webhookPayload = new BaseEventDTO();
      when(webhookClient.sendWebhook(anyString(), any(BaseEventDTO.class)))
              .thenThrow(new RuntimeException("Circuit breaker test exception"));
      when(meterRegistry.timer(anyString())).thenReturn(mock(Timer.class));
      when(Timer.start(any(MeterRegistry.class))).thenReturn(timer);

      // Act & Assert
      assertThatThrownBy(
              () -> webhookService.processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload))
              .isInstanceOf(RuntimeException.class)
              .hasMessage("Circuit breaker test exception");
      verify(meterRegistry).counter("webhook.failure");
      verify(meterRegistry).counter("webhook.circuit.open");
      verify(deadLetterQueueProducer).publish(DEAD_LETTER_QUEUE_TOPIC, EVENT_ID, eventPayload);
    }
  }
}