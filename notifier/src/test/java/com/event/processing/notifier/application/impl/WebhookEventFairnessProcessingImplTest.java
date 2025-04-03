package com.event.processing.notifier.application.impl;

import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import com.event.processing.notifier.producer.EventProducer;
import com.event.processing.notifier.service.DeduplicationService;
import com.event.processing.notifier.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookEventFairnessProcessingImplTest {

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private WebhookService webhookService;

    @Mock
    private EventProducer eventProducer;

    private WebhookEventFairnessProcessingImpl webhookEventProcessing;

    private static final String EVENT_ID = "test-event-id";
    private static final String WEBHOOK_URL = "http://test-webhook-url";
    private static final String WEBHOOK_EVENT_TOPIC = "test-webhook-topic";

    @BeforeEach
    void setUp() {
        webhookEventProcessing = new WebhookEventFairnessProcessingImpl(deduplicationService, webhookService, eventProducer);
        ReflectionTestUtils.setField(webhookEventProcessing, "webhookEventTopic", WEBHOOK_EVENT_TOPIC);
    }

    @Test
    void process_SuccessfulProcessing() {
        // Arrange
        WebhookEventDTO eventPayload = new WebhookEventDTO(EVENT_ID, "test-event-type", "test-account-id");
        BaseEventDTO webhookPayload = new BaseEventDTO();
        when(deduplicationService.isDuplicate(EVENT_ID)).thenReturn(false);
        doNothing().when(webhookService).processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

        // Act
        webhookEventProcessing.process(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

        // Assert
        verify(deduplicationService).isDuplicate(EVENT_ID);
        verify(webhookService).processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);
        verify(deduplicationService).markProcessed(EVENT_ID);
        verifyNoMoreInteractions(eventProducer); // EventProducer should not be used in successful case
    }

    @Test
    void process_DuplicateEvent() {
        // Arrange
        WebhookEventDTO eventPayload = new WebhookEventDTO(EVENT_ID, "test-event-type", "test-account-id");
        BaseEventDTO webhookPayload = new BaseEventDTO();
        when(deduplicationService.isDuplicate(EVENT_ID)).thenReturn(true);

        // Act
        webhookEventProcessing.process(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

        // Assert
        verify(deduplicationService).isDuplicate(EVENT_ID);
        verifyNoInteractions(webhookService);
        verifyNoMoreInteractions(deduplicationService);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void process_ProcessingFailure() {
        // Arrange
        WebhookEventDTO eventPayload = new WebhookEventDTO(EVENT_ID, "test-event-type", "test-account-id");
        BaseEventDTO webhookPayload = new BaseEventDTO();
        when(deduplicationService.isDuplicate(EVENT_ID)).thenReturn(false);
        doThrow(new RuntimeException("Test exception")).when(webhookService).processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

        // Act
        webhookEventProcessing.process(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);

        // Assert
        verify(deduplicationService).isDuplicate(EVENT_ID);
        verify(webhookService).processWithRetry(EVENT_ID, eventPayload, WEBHOOK_URL, webhookPayload);
        verifyNoMoreInteractions(deduplicationService); // markProcessed should not be called on failure
        verifyNoMoreInteractions(eventProducer); // EventProducer is not used in the current implementation on failure
    }
} 