package com.event.processing.notifier.consumer;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

/**
 * Interface defining the contract for consuming webhook events from a message
 * broker.
 * This interface provides a standardized way to process webhook events in a
 * batch manner
 * with acknowledgment support.
 *
 * Key features:
 * - Batch processing of webhook events
 * - Manual acknowledgment support
 * - Kafka consumer record handling
 *
 * @author LongLe
 * @version 1.0
 */
public interface EventConsumer {
  /**
   * Consumes a batch of webhook events from the message broker.
   * Implementations should process the events and acknowledge their successful
   * processing
   * using the provided acknowledgment object.
   *
   * @param records        List of consumer records containing webhook events to
   *                       process
   * @param acknowledgment Acknowledgment object to mark successful processing of
   *                       the batch
   */
  void consume(List<ConsumerRecord<String, WebhookEventDTO>> records, Acknowledgment acknowledgment);
}
