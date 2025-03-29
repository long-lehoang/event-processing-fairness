package com.event.processing.notifier.producer;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka-based implementation of the DeadLetterQueueProducer interface for
 * handling failed webhook events.
 * This class manages the asynchronous publishing of failed events to a
 * Kafka-based dead letter queue,
 * providing detailed logging and error handling capabilities.
 * <p>
 * Key features:
 * - Asynchronous DLQ publishing
 * - Detailed logging of failed events
 * - Error handling and reporting
 * - Kafka metadata tracking
 *
 * @author LongLe
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeadLetterQueueProducer implements DeadLetterQueueProducer {

  private final KafkaTemplate<String, WebhookEventDTO> kafkaTemplate;

  /**
   * Publishes a failed webhook event to the Kafka dead letter queue
   * asynchronously.
   * The method uses KafkaTemplate to send the event and provides detailed logging
   * of the publishing process, including success and failure scenarios.
   *
   * @param topic   The dead letter queue topic to publish the event to
   * @param id      The unique identifier for the failed event
   * @param payload The webhook event payload that failed processing
   */
  @Override
  public void publish(String topic, String id, WebhookEventDTO payload) {
    log.info("Publishing event to DLQ: Topic={}, Key={}, Payload={}", topic, id, payload);

    CompletableFuture<SendResult<String, WebhookEventDTO>> future = kafkaTemplate.send(topic, id, payload);

    future.thenAccept(result -> {
      RecordMetadata metadata = result.getRecordMetadata();
      log.info("DLQ Event Sent Successfully: Topic={}, Partition={}, Offset={}",
          metadata.topic(), metadata.partition(), metadata.offset());
    }).exceptionally(ex -> {
      log.error("Failed to publish event to DLQ: Topic={}, Key={}, Error={}",
          topic, id, ex.getMessage(), ex);
      return null;
    });
  }
}
