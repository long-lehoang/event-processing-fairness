package com.event.processing.dlq_service.consumer;

import com.event.processing.dlq_service.constants.MetricConstants;
import com.event.processing.dlq_service.domain.dto.DeadLetterQueueEventDTO;
import com.event.processing.dlq_service.service.DeadLetterQueueEventProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumer for dead letter queue events.
 * This class is responsible for consuming messages from the dead letter queue topic
 * and processing them using the DeadLetterQueueEventProcessor.
 * Following the Single Responsibility Principle, this class only handles
 * the consumption of dead letter queue events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueueConsumer {
  private final DeadLetterQueueEventProcessor dlqProcessor;
  private final MeterRegistry meterRegistry;

  /**
   * Consumes batches of messages from the dead letter queue topic.
   * Processes the batch of events using the DeadLetterQueueEventProcessor.
   * Uses ConsumerRecord to access complete message information including key, headers, and offset.
   *
   * @param records        The batch of consumer records from the dead letter queue topic
   * @param acknowledgment The acknowledgment callback for manual offset commit
   */
  @KafkaListener(
      topics = "${spring.kafka.topic.dead-letter-queue}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory"
  )
  public void consumeBatch(List<ConsumerRecord<String, DeadLetterQueueEventDTO>> records, Acknowledgment acknowledgment) {
    try {
      long startTime = System.currentTimeMillis();
      log.info("Received batch of {} DLQ records from Kafka", records.size());

      // Extract events from consumer records
      List<DeadLetterQueueEventDTO> events = new ArrayList<>(records.size());
      for (ConsumerRecord<String, DeadLetterQueueEventDTO> record : records) {
        DeadLetterQueueEventDTO event = record.value();

        // Log detailed information about the record
        log.debug("Processing record: topic={}, partition={}, offset={}, key={}, eventId={}",
            record.topic(), record.partition(), record.offset(), record.key(),
            event != null ? event.getEventId() : "null");

        if (event != null) {
          events.add(event);
        } else {
          log.warn("Received null event at offset {} in partition {}",
              record.offset(), record.partition());
        }
      }

      if (!events.isEmpty()) {
        // Process the batch of events
        dlqProcessor.handleDeadLetterEvents(events);

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Processed batch of {} DLQ events in {} ms", events.size(), processingTime);

        // Record metrics
        meterRegistry.counter(MetricConstants.DLQ_MESSAGES_BATCH_PROCESSED).increment();
        meterRegistry.gauge(MetricConstants.DLQ_MESSAGES_BATCH_SIZE, events.size());
        meterRegistry.timer(MetricConstants.DLQ_MESSAGES_BATCH_PROCESSING_TIME).record(processingTime, java.util.concurrent.TimeUnit.MILLISECONDS);
      } else {
        log.warn("No valid events found in the batch of {} records", records.size());
      }

      // Acknowledge the batch after processing
      acknowledgment.acknowledge();
    } catch (Exception e) {
      log.error("Error processing DLQ batch: {}", e.getMessage(), e);
      meterRegistry.counter(MetricConstants.DLQ_MESSAGES_BATCH_FAILED).increment();

      // In case of error, we can choose to acknowledge the batch anyway to move forward,
      // or not acknowledge to retry the entire batch. Here we acknowledge to avoid getting stuck.
      acknowledgment.acknowledge();
    }
  }
}