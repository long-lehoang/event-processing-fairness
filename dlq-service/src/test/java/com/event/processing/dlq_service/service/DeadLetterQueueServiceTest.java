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
    service = new DeadLetterQueueService(repository, objectMapper, meterRegistry);

    ReflectionTestUtils.setField(service, "maxRetryAttempts", 3);
    ReflectionTestUtils.setField(service, "initialDelaySeconds", 300L);
  }

  @Test
  void handleDeadLetterEvents_shouldProcessBatchOfEvents() throws Exception {
    // Arrange
    DeadLetterQueueEventDTO event1 = createEvent("event-1", "account-1", "type-1", "error-1");
    DeadLetterQueueEventDTO event2 = createEvent("event-2", "account-2", "type-2", "error-2");

    List<DeadLetterQueueEventDTO> events = Arrays.asList(event1, event2);

    lenient().when(repository.findById(anyString())).thenReturn(Optional.empty());

    // Act
    service.handleDeadLetterEvents(events);

    // Assert
    verify(repository, times(1)).saveAll(any());
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

  private DeadLetterQueueEventDTO createEvent(String eventId, String accountId, String eventType, String failureReason) {
    DeadLetterQueueEventDTO event = new DeadLetterQueueEventDTO();
    event.setEventId(eventId);
    event.setAccountId(accountId);
    event.setEventType(eventType);
    event.setFailureReason(failureReason);
    return event;
  }
}
