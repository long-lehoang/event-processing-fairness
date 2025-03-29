package com.event.processing.notifier.producer;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka-based implementation of the EventProducer interface for publishing
 * webhook events.
 * This class handles the asynchronous publishing of webhook events to Kafka
 * topics
 * with monitoring and error handling capabilities.
 * <p>
 * Key features:
 * - Asynchronous event publishing
 * - Performance monitoring with metrics
 * - Detailed logging of publish operations
 * - Error handling and reporting
 *
 * @author LongLe
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducer implements EventProducer {

  private final KafkaTemplate<String, WebhookEventDTO> kafkaTemplate;
  private final MeterRegistry meterRegistry;

  /**
   * Publishes a webhook event to the specified Kafka topic asynchronously.
   * The method uses KafkaTemplate to send the event and provides monitoring
   * through metrics and logging.
   *
   * @param topic   The Kafka topic to publish the event to
   * @param key     The unique key for the event
   * @param payload The webhook event payload to publish
   */
  @Override
  public void publish(String topic, String key, WebhookEventDTO payload) {
    log.info("Publishing event to Kafka. Topic: {}, Key: {}", topic, key);

    CompletableFuture<SendResult<String, WebhookEventDTO>> future = kafkaTemplate.send(topic, key, payload);

    future.thenAccept(result -> {
      RecordMetadata metadata = result.getRecordMetadata();
      log.info("Successfully published event. Topic: {}, Partition: {}, Offset: {}, Key: {}",
          topic, metadata.partition(), metadata.offset(), key);
      meterRegistry.counter("kafka.publish.success", "topic", topic).increment();
    }).exceptionally(ex -> {
      log.error("Failed to publish event. Topic: {}, Key: {}, Error: {}", topic, key, ex.getMessage(), ex);
      meterRegistry.counter("kafka.publish.failure", "topic", topic).increment();
      return null;
    });
  }
}
