package com.event.processing.dlq_service.consumer;

import com.event.processing.dlq_service.domain.dto.DeadLetterQueueEventDTO;
import com.event.processing.dlq_service.service.DeadLetterQueueEventProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the DeadLetterQueueConsumer class.
 * Tests the batch processing functionality using ConsumerRecord.
 */
@ExtendWith(MockitoExtension.class)
class DeadLetterQueueConsumerTest {

  @Mock
  private DeadLetterQueueEventProcessor dlqProcessor;

  @Mock
  private Acknowledgment acknowledgment;

  private MeterRegistry meterRegistry;
  private DeadLetterQueueConsumer consumer;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    consumer = new DeadLetterQueueConsumer(dlqProcessor, meterRegistry);
  }

  @Test
  void consumeBatch_shouldProcessBatchAndAcknowledge() {
    // Arrange
    String topic = "test-topic";
    List<ConsumerRecord<String, DeadLetterQueueEventDTO>> records = Arrays.asList(
        createConsumerRecord(topic, 0, 0, "key-1", createEvent("event-1", "account-1", "type-1", "error-1")),
        createConsumerRecord(topic, 0, 1, "key-2", createEvent("event-2", "account-2", "type-2", "error-2"))
    );

    // Act
    consumer.consumeBatch(records, acknowledgment);

    // Assert
    ArgumentCaptor<List<DeadLetterQueueEventDTO>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(dlqProcessor, times(1)).handleDeadLetterEvents(eventsCaptor.capture());
    verify(acknowledgment, times(1)).acknowledge();

    List<DeadLetterQueueEventDTO> capturedEvents = eventsCaptor.getValue();
    assertEquals(2, capturedEvents.size());
    assertEquals("event-1", capturedEvents.get(0).getEventId());
    assertEquals("event-2", capturedEvents.get(1).getEventId());
  }

  @Test
  void consumeBatch_shouldHandleEmptyBatch() {
    // Arrange
    List<ConsumerRecord<String, DeadLetterQueueEventDTO>> records = Collections.emptyList();

    // Act
    consumer.consumeBatch(records, acknowledgment);

    // Assert
    verify(dlqProcessor, never()).handleDeadLetterEvents(any());
    verify(acknowledgment, times(1)).acknowledge();
  }

  @Test
  void consumeBatch_shouldHandleNullEvents() {
    // Arrange
    String topic = "test-topic";
    List<ConsumerRecord<String, DeadLetterQueueEventDTO>> records = Arrays.asList(
        createConsumerRecord(topic, 0, 0, "key-1", null),
        createConsumerRecord(topic, 0, 1, "key-2", createEvent("event-2", "account-2", "type-2", "error-2"))
    );

    // Act
    consumer.consumeBatch(records, acknowledgment);

    // Assert
    ArgumentCaptor<List<DeadLetterQueueEventDTO>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(dlqProcessor, times(1)).handleDeadLetterEvents(eventsCaptor.capture());
    verify(acknowledgment, times(1)).acknowledge();

    List<DeadLetterQueueEventDTO> capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    assertEquals("event-2", capturedEvents.get(0).getEventId());
  }

  @Test
  void consumeBatch_shouldHandleExceptionAndAcknowledge() {
    // Arrange
    String topic = "test-topic";
    List<ConsumerRecord<String, DeadLetterQueueEventDTO>> records = Arrays.asList(
        createConsumerRecord(topic, 0, 0, "key-1", createEvent("event-1", "account-1", "type-1", "error-1"))
    );

    doThrow(new RuntimeException("Test exception"))
        .when(dlqProcessor).handleDeadLetterEvents(any());

    // Act
    consumer.consumeBatch(records, acknowledgment);

    // Assert
    verify(dlqProcessor, times(1)).handleDeadLetterEvents(any());
    verify(acknowledgment, times(1)).acknowledge();
  }

  private DeadLetterQueueEventDTO createEvent(String eventId, String accountId, String eventType, String failureReason) {
    return DeadLetterQueueEventDTO.builder()
        .eventId(eventId)
        .accountId(accountId)
        .eventType(eventType)
        .failureReason(failureReason)
        .build();
  }

  private ConsumerRecord<String, DeadLetterQueueEventDTO> createConsumerRecord(
      String topic, int partition, long offset, String key, DeadLetterQueueEventDTO value) {
    return new ConsumerRecord<>(
        topic,
        partition,
        offset,
        System.currentTimeMillis(),
        null, // timestamp type
        0L, // checksum
        0, // serialized key size
        0, // serialized value size
        key,
        value,
        new RecordHeaders()
    );
  }
}
