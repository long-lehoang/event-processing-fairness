package com.event.processing.dlq_service.producer;

import com.event.processing.dlq_service.domain.dto.WebhookEventDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the WebhookEventProducer class.
 * Tests the functionality of publishing webhook events to Kafka.
 */
@ExtendWith(MockitoExtension.class)
class WebhookEventProducerTest {

  @Mock
  private KafkaTemplate<String, WebhookEventDTO> kafkaTemplate;

  private MeterRegistry meterRegistry;
  private WebhookEventProducer webhookEventProducer;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    webhookEventProducer = new WebhookEventProducer(kafkaTemplate, meterRegistry);
    ReflectionTestUtils.setField(webhookEventProducer, "webhookEventsTopic", "webhook-events");
  }

  @Test
  void publishEvent_shouldSendMessageToKafka() {
    // Arrange
    WebhookEventDTO event = WebhookEventDTO.builder()
        .eventId("test-event-id")
        .accountId("test-account-id")
        .eventType("test-event-type")
        .build();

    CompletableFuture<SendResult<String, WebhookEventDTO>> future = new CompletableFuture<>();
    when(kafkaTemplate.send(anyString(), anyString(), any(WebhookEventDTO.class))).thenReturn(future);

    // Act
    webhookEventProducer.publishEvent(event);

    // Assert
    verify(kafkaTemplate, times(1)).send(eq("webhook-events"), eq("test-event-id"), eq(event));
  }

  @Test
  void publishEventWithTopic_shouldSendMessageToSpecificTopic() {
    // Arrange
    String customTopic = "custom-topic";
    WebhookEventDTO event = WebhookEventDTO.builder()
        .eventId("test-event-id")
        .accountId("test-account-id")
        .eventType("test-event-type")
        .build();

    CompletableFuture<SendResult<String, WebhookEventDTO>> future = new CompletableFuture<>();
    when(kafkaTemplate.send(anyString(), anyString(), any(WebhookEventDTO.class))).thenReturn(future);

    // Act
    webhookEventProducer.publishEvent(customTopic, event);

    // Assert
    verify(kafkaTemplate, times(1)).send(eq(customTopic), eq("test-event-id"), eq(event));
  }
}
