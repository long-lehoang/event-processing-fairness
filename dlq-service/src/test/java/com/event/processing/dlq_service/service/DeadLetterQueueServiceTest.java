package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.constants.EventStatusConstants;
import com.event.processing.dlq_service.constants.MetricConstants;
import com.event.processing.dlq_service.domain.dto.DeadLetterQueueEventDTO;
import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import com.event.processing.dlq_service.repository.DeadLetterEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueueServiceTest {

  @Mock
  private DeadLetterEventRepository repository;

  @Mock
  private RetryService retryService;

  private ObjectMapper objectMapper;
  private MeterRegistry meterRegistry;
  private DeadLetterQueueService service;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    meterRegistry = new SimpleMeterRegistry();
    service = new DeadLetterQueueService(repository, objectMapper, meterRegistry, retryService);

    ReflectionTestUtils.setField(service, "maxRetryAttempts", 3);
    ReflectionTestUtils.setField(service, "initialDelaySeconds", 300L);
  }

  @Test
  void handleDeadLetterEvent_shouldCreateNewEventWhenNotExists() throws Exception {
    // Arrange
    String eventId = "test-event-id";
    String accountId = "test-account-id";
    String eventType = "test-event-type";
    String failureReason = "test-failure-reason";
    String payload = "{\"eventId\":\"test-event-id\"}";

    when(repository.findById(eventId)).thenReturn(Optional.empty());

    // Act
    service.handleDeadLetterEvent(eventId, accountId, eventType, payload, failureReason);

    // Assert
    ArgumentCaptor<DeadLetterEvent> eventCaptor = ArgumentCaptor.forClass(DeadLetterEvent.class);
    verify(repository).save(eventCaptor.capture());

    DeadLetterEvent savedEvent = eventCaptor.getValue();
    assertEquals(eventId, savedEvent.getEventId());
    assertEquals(accountId, savedEvent.getAccountId());
    assertEquals(eventType, savedEvent.getEventType());
    assertEquals(payload, savedEvent.getPayload());
    assertEquals(failureReason, savedEvent.getFailureReason());
    assertEquals(EventStatusConstants.PENDING, savedEvent.getStatus());
    assertEquals(0, savedEvent.getRetryCount());
  }

  @Test
  void handleDeadLetterEvent_shouldUpdateExistingEvent() {
    // Arrange
    String eventId = "test-event-id";
    String accountId = "test-account-id";
    String eventType = "test-event-type";
    String failureReason = "test-failure-reason";
    String payload = "{\"eventId\":\"test-event-id\"}";

    DeadLetterEvent existingEvent = new DeadLetterEvent();
    existingEvent.setEventId(eventId);
    existingEvent.setRetryCount(1);

    when(repository.findById(eventId)).thenReturn(Optional.of(existingEvent));

    // Act
    service.handleDeadLetterEvent(eventId, accountId, eventType, payload, failureReason);

    // Assert
    ArgumentCaptor<DeadLetterEvent> eventCaptor = ArgumentCaptor.forClass(DeadLetterEvent.class);
    verify(repository).save(eventCaptor.capture());

    DeadLetterEvent savedEvent = eventCaptor.getValue();
    assertEquals(eventId, savedEvent.getEventId());
    assertEquals(2, savedEvent.getRetryCount());
    assertEquals(failureReason, savedEvent.getLastErrorMessage());
  }

  @Test
  void handleDeadLetterEvents_shouldProcessBatchOfEvents() throws Exception {
    // Arrange
    DeadLetterQueueEventDTO event1 = createEvent("event-1", "account-1", "type-1", "error-1");
    DeadLetterQueueEventDTO event2 = createEvent("event-2", "account-2", "type-2", "error-2");

    List<DeadLetterQueueEventDTO> events = Arrays.asList(event1, event2);

    when(repository.findById(anyString())).thenReturn(Optional.empty());

    // Act
    service.handleDeadLetterEvents(events);

    // Assert
    verify(repository, times(2)).save(any(DeadLetterEvent.class));
    verify(meterRegistry).counter(MetricConstants.DLQ_EVENTS_BATCH_RECEIVED);
    verify(meterRegistry).counter(MetricConstants.DLQ_EVENTS_BATCH_PROCESSED);
  }

  @Test
  void handleDeadLetterEvents_shouldHandleEmptyList() {
    // Arrange
    List<DeadLetterQueueEventDTO> events = Collections.emptyList();

    // Act
    service.handleDeadLetterEvents(events);

    // Assert
    verify(repository, never()).save(any(DeadLetterEvent.class));
  }

  @Test
  void processDeadLetterQueueEvent_shouldProcessEvent() throws Exception {
    // Arrange
    DeadLetterQueueEventDTO event = createEvent("event-1", "account-1", "type-1", "error-1");

    when(repository.findById(anyString())).thenReturn(Optional.empty());

    // Use reflection to access the private method
    java.lang.reflect.Method method = DeadLetterQueueService.class.getDeclaredMethod(
        "processDeadLetterQueueEvent", DeadLetterQueueEventDTO.class);
    method.setAccessible(true);

    // Act
    method.invoke(service, event);

    // Assert
    verify(repository).findById("event-1");
    verify(repository).save(any(DeadLetterEvent.class));
  }

  private DeadLetterQueueEventDTO createEvent(String eventId, String accountId, String eventType, String failureReason) {
    DeadLetterQueueEventDTO event = new DeadLetterQueueEventDTO();
    event.setEventId(eventId);
    event.setAccountId(accountId);
    event.setEventType(eventType);
    event.setFailureReason(failureReason);
    return event;
  }
}
