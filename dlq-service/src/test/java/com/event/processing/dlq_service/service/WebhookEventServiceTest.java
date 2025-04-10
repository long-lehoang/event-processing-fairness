package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.domain.dto.WebhookEventDTO;
import com.event.processing.dlq_service.producer.EventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the WebhookEventService class.
 * Tests the functionality of creating and publishing webhook events.
 */
@ExtendWith(MockitoExtension.class)
class WebhookEventServiceTest {

  @Mock
  private EventProducer<WebhookEventDTO> webhookEventProducer;

  private WebhookEventService webhookEventService;

  @BeforeEach
  void setUp() {
    webhookEventService = new WebhookEventService(webhookEventProducer);
  }

  @Test
  void createAndPublishEvent_shouldCreateAndPublishEvent() {
    // Arrange
    String eventType = "test-event-type";
    String accountId = "test-account-id";

    CompletableFuture<SendResult<String, WebhookEventDTO>> future = new CompletableFuture<>();
    when(webhookEventProducer.publishEvent(any(WebhookEventDTO.class))).thenReturn(future);

    // Act
    WebhookEventDTO result = webhookEventService.createAndPublishEvent(eventType, accountId);

    // Assert
    assertNotNull(result);
    assertEquals(eventType, result.getEventType());
    assertEquals(accountId, result.getAccountId());
    assertNotNull(result.getEventId());
    verify(webhookEventProducer, times(1)).publishEvent(any(WebhookEventDTO.class));
  }

  @Test
  void publishEvent_shouldPublishEvent() {
    // Arrange
    WebhookEventDTO event = WebhookEventDTO.builder()
        .eventId("test-event-id")
        .accountId("test-account-id")
        .eventType("test-event-type")
        .build();

    CompletableFuture<SendResult<String, WebhookEventDTO>> future = new CompletableFuture<>();
    when(webhookEventProducer.publishEvent(any(WebhookEventDTO.class))).thenReturn(future);

    // Act
    webhookEventService.publishEvent(event);

    // Assert
    verify(webhookEventProducer, times(1)).publishEvent(event);
  }

  @Test
  void publishEventWithTopic_shouldPublishEventToSpecificTopic() {
    // Arrange
    String topic = "custom-topic";
    WebhookEventDTO event = WebhookEventDTO.builder()
        .eventId("test-event-id")
        .accountId("test-account-id")
        .eventType("test-event-type")
        .build();

    CompletableFuture<SendResult<String, WebhookEventDTO>> future = new CompletableFuture<>();
    when(webhookEventProducer.publishEvent(anyString(), any(WebhookEventDTO.class))).thenReturn(future);

    // Act
    webhookEventService.publishEvent(topic, event);

    // Assert
    verify(webhookEventProducer, times(1)).publishEvent(eq(topic), eq(event));
  }
}
