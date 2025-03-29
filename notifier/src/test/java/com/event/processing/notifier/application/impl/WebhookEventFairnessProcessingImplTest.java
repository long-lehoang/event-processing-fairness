package com.event.processing.notifier.application.impl;

import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import com.event.processing.notifier.producer.EventProducer;
import com.event.processing.notifier.service.DeduplicationService;
import com.event.processing.notifier.service.RateLimiterService;
import com.event.processing.notifier.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookEventFairnessProcessingImplTest {

  private static final String EVENT_ID = "test-event-id";
  private static final String WEBHOOK_URL = "http://test-url.com";
  private static final String WEBHOOK_TOPIC = "webhook-events";
  @Mock
  private DeduplicationService deduplicationService;
  @Mock
  private RateLimiterService rateLimiterService;
  @Mock
  private WebhookService webhookService;
  @Mock
  private EventProducer eventProducer;
  @Captor
  private ArgumentCaptor<String> eventIdCaptor;
  private WebhookEventFairnessProcessingImpl processor;

  @BeforeEach
  void setUp() {
    processor = new WebhookEventFairnessProcessingImpl(
        deduplicationService,
        rateLimiterService,
        webhookService,
        eventProducer);
  }

  @Test
  void process_WhenEventIsDuplicate_ShouldSkipProcessing() {
    // Arrange
    WebhookEventDTO eventPayload = new WebhookEventDTO();
    BaseEventDTO webhookPayload = new BaseEventDTO();
    when(deduplicationService.isDuplicate(EVENT_ID)).thenReturn(true);

    // Act
    processor.process(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

    // Assert
    verify(deduplicationService).isDuplicate(EVENT_ID);
    verifyNoInteractions(rateLimiterService, webhookService, eventProducer);
  }

  @Test
  void process_WhenEventIsRateLimited_ShouldRepublishToQueue() {
    // Arrange
    WebhookEventDTO eventPayload = new WebhookEventDTO();
    BaseEventDTO webhookPayload = new BaseEventDTO();
    when(deduplicationService.isDuplicate(EVENT_ID)).thenReturn(false);
    when(rateLimiterService.isAllow(EVENT_ID)).thenReturn(false);

    // Act
    processor.process(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

    // Assert
    verify(deduplicationService).isDuplicate(EVENT_ID);
    verify(rateLimiterService).isAllow(EVENT_ID);
    verifyNoInteractions(webhookService);
  }

  @Test
  void process_WhenEventIsValid_ShouldProcessSuccessfully() {
    // Arrange
    WebhookEventDTO eventPayload = new WebhookEventDTO();
    BaseEventDTO webhookPayload = new BaseEventDTO();
    when(deduplicationService.isDuplicate(EVENT_ID)).thenReturn(false);
    when(rateLimiterService.isAllow(EVENT_ID)).thenReturn(true);
    doNothing().when(webhookService).processWithRetry(anyString(), any(), anyString(), any());

    // Act
    processor.process(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

    // Assert
    verify(deduplicationService).isDuplicate(EVENT_ID);
    verify(rateLimiterService).isAllow(EVENT_ID);
    verify(webhookService).processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);
    verify(deduplicationService).markProcessed(EVENT_ID);
    verifyNoInteractions(eventProducer);
  }

  @Test
  void process_WhenProcessingFails_ShouldLogErrorAndNotMarkAsProcessed() {
    // Arrange
    WebhookEventDTO eventPayload = new WebhookEventDTO();
    BaseEventDTO webhookPayload = new BaseEventDTO();
    when(deduplicationService.isDuplicate(EVENT_ID)).thenReturn(false);
    when(rateLimiterService.isAllow(EVENT_ID)).thenReturn(true);
    doThrow(new RuntimeException("Processing failed"))
        .when(webhookService).processWithRetry(anyString(), any(), anyString(), any());

    // Act
    processor.process(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

    // Assert
    verify(deduplicationService).isDuplicate(EVENT_ID);
    verify(rateLimiterService).isAllow(EVENT_ID);
    verify(webhookService).processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);
    verify(deduplicationService, never()).markProcessed(anyString());
    verifyNoInteractions(eventProducer);
  }
}