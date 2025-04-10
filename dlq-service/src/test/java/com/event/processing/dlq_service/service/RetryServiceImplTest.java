package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.constants.EventStatusConstants;
import com.event.processing.dlq_service.constants.MetricConstants;
import com.event.processing.dlq_service.domain.dto.WebhookEventDTO;
import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import com.event.processing.dlq_service.domain.mapper.DeadLetterQueueMapper;
import com.event.processing.dlq_service.producer.EventProducer;
import com.event.processing.dlq_service.repository.DeadLetterEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryServiceImplTest {

  @Mock
  private DeadLetterEventRepository repository;

  @Mock
  private EventProducer<WebhookEventDTO> webhookEventProducer;

  @Mock
  private DeadLetterQueueMapper mapper;

  private MeterRegistry meterRegistry;
  private Executor testExecutor;
  private RetryServiceImpl retryService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    testExecutor = Executors.newFixedThreadPool(2);
    retryService = new RetryServiceImpl(repository, webhookEventProducer, mapper, meterRegistry, testExecutor);

    ReflectionTestUtils.setField(retryService, "webhookEventsTopic", "webhook-events");
    ReflectionTestUtils.setField(retryService, "maxRetryAttempts", 5);
    ReflectionTestUtils.setField(retryService, "initialDelaySeconds", 300L);
    ReflectionTestUtils.setField(retryService, "multiplier", 2.0);
    ReflectionTestUtils.setField(retryService, "batchSize", 10);
  }

  @Test
  void retryEvent_shouldPublishEventAndUpdateStatus() {
    // Arrange
    DeadLetterEvent event = new DeadLetterEvent();
    event.setEventId("test-event-id");
    event.setRetryCount(1);

    WebhookEventDTO webhookEvent = new WebhookEventDTO();
    webhookEvent.setEventId("test-event-id");

    when(mapper.toWebhookEventDTO(event)).thenReturn(webhookEvent);
    when(webhookEventProducer.publishEvent(anyString(), any(WebhookEventDTO.class)))
        .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

    // Act
    CompletableFuture<Void> result = retryService.retryEvent(event);

    // Assert
    assertTrue(result.isDone());
    verify(webhookEventProducer).publishEvent(eq("webhook-events"), eq(webhookEvent));
    verify(repository).save(event);
    assertEquals(EventStatusConstants.RETRYING, event.getStatus());
    verify(meterRegistry).counter(MetricConstants.DLQ_EVENTS_RETRIED);
  }

  @Test
  void processRetries_shouldProcessEventsInBatches() {
    // Arrange
    String status = EventStatusConstants.PENDING;
    Instant now = Instant.now();

    DeadLetterEvent event1 = new DeadLetterEvent();
    event1.setEventId("event-1");

    DeadLetterEvent event2 = new DeadLetterEvent();
    event2.setEventId("event-2");

    Page<DeadLetterEvent> page1 = new PageImpl<>(Arrays.asList(event1, event2));
    Page<DeadLetterEvent> emptyPage = new PageImpl<>(Collections.emptyList());

    when(repository.findByStatusAndNextRetryAtBeforePaged(eq(status), eq(now), any(Pageable.class)))
        .thenReturn(page1)
        .thenReturn(emptyPage);

    WebhookEventDTO webhookEvent = new WebhookEventDTO();
    when(mapper.toWebhookEventDTO(any(DeadLetterEvent.class))).thenReturn(webhookEvent);
    when(webhookEventProducer.publishEvent(anyString(), any(WebhookEventDTO.class)))
        .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

    // Act
    List<String> result = retryService.processRetries(status, now);

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.contains("event-1"));
    assertTrue(result.contains("event-2"));
    verify(repository, times(2)).save(any(DeadLetterEvent.class));
  }

  @Test
  void processRetriesConcurrently_shouldUseMultipleThreads() {
    // Arrange
    String status = EventStatusConstants.PENDING;
    Instant now = Instant.now();
    int concurrencyLevel = 2;

    DeadLetterEvent event1 = new DeadLetterEvent();
    event1.setEventId("event-1");

    DeadLetterEvent event2 = new DeadLetterEvent();
    event2.setEventId("event-2");

    Page<DeadLetterEvent> page1 = new PageImpl<>(Arrays.asList(event1, event2));
    Page<DeadLetterEvent> emptyPage = new PageImpl<>(Collections.emptyList());

    when(repository.findByStatusAndNextRetryAtBeforePaged(eq(status), eq(now), any(Pageable.class)))
        .thenReturn(page1)
        .thenReturn(emptyPage);

    WebhookEventDTO webhookEvent = new WebhookEventDTO();
    when(mapper.toWebhookEventDTO(any(DeadLetterEvent.class))).thenReturn(webhookEvent);
    when(webhookEventProducer.publishEvent(anyString(), any(WebhookEventDTO.class)))
        .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

    // Act
    List<String> result = retryService.processRetriesConcurrently(status, now, concurrencyLevel);

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.contains("event-1"));
    assertTrue(result.contains("event-2"));
    verify(repository, times(2)).save(any(DeadLetterEvent.class));
  }

  @Test
  void processAndReturnEventId_shouldReturnEventIdOnSuccess() throws Exception {
    // Arrange
    DeadLetterEvent event = new DeadLetterEvent();
    event.setEventId("test-event-id");

    WebhookEventDTO webhookEvent = new WebhookEventDTO();
    when(mapper.toWebhookEventDTO(event)).thenReturn(webhookEvent);
    when(webhookEventProducer.publishEvent(anyString(), any(WebhookEventDTO.class)))
        .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

    // Use reflection to access the private method
    java.lang.reflect.Method method = RetryServiceImpl.class.getDeclaredMethod(
        "processAndReturnEventId", DeadLetterEvent.class);
    method.setAccessible(true);

    // Act
    String result = (String) method.invoke(retryService, event);

    // Assert
    assertEquals("test-event-id", result);
    verify(webhookEventProducer).publishEvent(eq("webhook-events"), eq(webhookEvent));
  }

  @Test
  void processAndReturnEventId_shouldReturnNullOnFailure() throws Exception {
    // Arrange
    DeadLetterEvent event = new DeadLetterEvent();
    event.setEventId("test-event-id");

    WebhookEventDTO webhookEvent = new WebhookEventDTO();
    when(mapper.toWebhookEventDTO(event)).thenReturn(webhookEvent);
    when(webhookEventProducer.publishEvent(anyString(), any(WebhookEventDTO.class)))
        .thenThrow(new RuntimeException("Test exception"));

    // Use reflection to access the private method
    java.lang.reflect.Method method = RetryServiceImpl.class.getDeclaredMethod(
        "processAndReturnEventId", DeadLetterEvent.class);
    method.setAccessible(true);

    // Act
    String result = (String) method.invoke(retryService, event);

    // Assert
    assertNull(result);
    verify(webhookEventProducer).publishEvent(eq("webhook-events"), eq(webhookEvent));
  }
}
