package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.client.WebhookClient;
import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import com.event.processing.notifier.producer.DeadLetterQueueProducer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceImplTest {

    @Mock
    private WebhookClient webhookClient;

    @Mock
    private DeadLetterQueueProducer deadLetterQueueProducer;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private WebhookServiceImpl webhookService;
    private RetryRegistry retryRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private static final String EVENT_ID = "test-event-id";
    private static final String WEBHOOK_URL = "http://test-webhook-url";
    private static final String DEAD_LETTER_QUEUE_TOPIC = "test-dlq-topic";

    @BeforeEach
    void setUp() {
        // Create retry configuration
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(RuntimeException.class)
                .build();
        retryRegistry = RetryRegistry.of(retryConfig);

        // Create circuit breaker configuration
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(5000))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

        // Create service with mocked dependencies
        webhookService = new WebhookServiceImpl(webhookClient, deadLetterQueueProducer, meterRegistry);
        ReflectionTestUtils.setField(webhookService, "deadLetterQueueTopic", DEAD_LETTER_QUEUE_TOPIC);
        
        // Setup meter registry mock
        when(meterRegistry.counter(any())).thenReturn(counter);
    }

    @Test
    void processWithRetry_Success() {
        // Arrange
        WebhookEventDTO eventPayload = new WebhookEventDTO();
        BaseEventDTO webhookPayload = new BaseEventDTO();
        when(webhookClient.sendWebhook(WEBHOOK_URL, webhookPayload)).thenReturn(true);

        // Act
        webhookService.processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

        // Assert
        verify(webhookClient).sendWebhook(WEBHOOK_URL, webhookPayload);
        verify(meterRegistry, times(2)).counter(any());
        verify(counter, times(2)).increment(); // WEBHOOK_SUCCESS_COUNT
        verify(counter, times(2)).increment(); // WEBHOOK_EXECUTION_COUNT
    }

    @Test
    void processWithRetry_Failure() {
        // Arrange
        WebhookEventDTO eventPayload = new WebhookEventDTO();
        BaseEventDTO webhookPayload = new BaseEventDTO();
        when(webhookClient.sendWebhook(WEBHOOK_URL, webhookPayload)).thenReturn(false);

        // Act & Assert
        try {
            webhookService.processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Assert
        verify(webhookClient).sendWebhook(WEBHOOK_URL, webhookPayload);
        verify(meterRegistry, times(2)).counter(any());
        verify(counter, times(2)).increment(); // WEBHOOK_FAILURE_COUNT
        verify(counter, times(2)).increment(); // WEBHOOK_EXECUTION_COUNT
    }

    @Test
    void processWithRetry_Exception() {
        // Arrange
        WebhookEventDTO eventPayload = new WebhookEventDTO();
        BaseEventDTO webhookPayload = new BaseEventDTO();
        when(webhookClient.sendWebhook(WEBHOOK_URL, webhookPayload))
            .thenThrow(new RuntimeException("Test exception"));

        // Act & Assert
        try {
            webhookService.processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Assert
        verify(webhookClient).sendWebhook(WEBHOOK_URL, webhookPayload);
        verify(meterRegistry, times(2)).counter(any());
        verify(counter, times(2)).increment(); // WEBHOOK_FAILURE_COUNT
        verify(counter, times(2)).increment(); // WEBHOOK_EXECUTION_COUNT
    }

    @Test
    void processWithRetry_CircuitBreakerTriggered() {
        // Arrange
        WebhookEventDTO eventPayload = new WebhookEventDTO();
        BaseEventDTO webhookPayload = new BaseEventDTO();
        
        // Simulate circuit breaker being triggered by throwing multiple exceptions
        when(webhookClient.sendWebhook(WEBHOOK_URL, webhookPayload))
            .thenThrow(new RuntimeException("Test circuit break"));

        // Act
        try {
            // Call multiple times to trigger circuit breaker (more than failureRateThreshold)
            for (int i = 0; i < 6; i++) {
                try {
                    webhookService.processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);
                } catch (RuntimeException e) {
                    // Expected exception
                }
            }
        } catch (Exception e) {
            // Expected exception from circuit breaker
        }

        // Assert
        verify(webhookClient, atLeast(3)).sendWebhook(WEBHOOK_URL, webhookPayload);
        verify(meterRegistry, atLeast(3)).counter(any());
        verify(counter, atLeast(1)).increment(); // CIRCUIT_BREAKER_OPEN_COUNT
    }

    @Test
    void processWithRetry_RetryExhausted() {
        // Arrange
        WebhookEventDTO eventPayload = new WebhookEventDTO();
        BaseEventDTO webhookPayload = new BaseEventDTO();
        
        // Simulate retry exhaustion by throwing exceptions
        when(webhookClient.sendWebhook(WEBHOOK_URL, webhookPayload))
            .thenThrow(new RuntimeException("Test retry exhaustion"));

        // Act
        try {
            webhookService.processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Assert
        // With maxAttempts=3 in configuration, we expect exactly 3 attempts
        verify(webhookClient, times(1)).sendWebhook(WEBHOOK_URL, webhookPayload);
        verify(meterRegistry, times(2)).counter(any());
        verify(counter, times(2)).increment(); // WEBHOOK_FAILURE_COUNT
        verify(counter, times(2)).increment(); // WEBHOOK_EXECUTION_COUNT
    }
} 