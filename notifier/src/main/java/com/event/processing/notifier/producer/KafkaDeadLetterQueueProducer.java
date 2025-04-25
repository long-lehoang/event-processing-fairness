package com.event.processing.notifier.producer;

import com.event.processing.notifier.converter.WebhookToDLQEventConverter;
import com.event.processing.notifier.domain.dto.DeadLetterQueueEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka-based implementation of the DeadLetterQueueProducer interface for
 * handling failed events.
 *
 * This class manages the asynchronous publishing of failed events to a
 * Kafka-based dead letter queue, providing detailed logging and error handling capabilities.
 *
 * Key features:
 * - Asynchronous DLQ publishing
 * - Detailed logging of failed events
 * - Error handling and reporting
 * - Kafka metadata tracking
 * - Support for both DeadLetterQueueEventDTO and WebhookEventDTO formats
 * - Conversion between event formats
 *
 * @author LongLe
 * @version 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeadLetterQueueProducer implements DeadLetterQueueProducer {

  private final KafkaTemplate<String, DeadLetterQueueEventDTO> dlqKafkaTemplate;
  private final WebhookToDLQEventConverter converter;

  @Override
  public void publish(String topic, String key, DeadLetterQueueEventDTO payload) {
    log.info("[DLQ] Publishing to topic: {}, key: {}", topic, key);

    CompletableFuture<SendResult<String, DeadLetterQueueEventDTO>> future =
        dlqKafkaTemplate.send(topic, key, payload);

    handleFuture(future, topic, key, "[DLQ]");
  }

  @Override
  public void publishWithFailureReason(String topic, String key, WebhookEventDTO payload, String failureReason) {
    log.info("[With Reason] Publishing webhook event to DLQ: Topic={}, Key={}, Reason={}",
        topic, key, failureReason);

    DeadLetterQueueEventDTO dlqEvent = converter.convert(payload, failureReason);
    publish(topic, key, dlqEvent);
  }

  /**
   * Handles the CompletableFuture returned by Kafka send operations.
   * This method provides consistent logging and error handling for all Kafka operations.
   *
   * @param future    The CompletableFuture returned by the Kafka send operation
   * @param topic     The topic the message was sent to
   * @param key       The key used for the message
   * @param logPrefix A prefix to use in log messages for context
   * @param <T>       The type of payload that was sent
   */
  private <T> void handleFuture(CompletableFuture<SendResult<String, T>> future,
                               String topic, String key, String logPrefix) {
    future.thenAccept(result -> {
      var metadata = result.getRecordMetadata();
      log.info("{}Message sent successfully: topic={}, partition={}, offset={}",
          logPrefix, metadata.topic(), metadata.partition(), metadata.offset());
    }).exceptionally(ex -> {
      log.error("{}Failed to publish message: topic={}, key={}, error={}",
          logPrefix, topic, key, ex.getMessage(), ex);
      return null;
    });
  }
}
