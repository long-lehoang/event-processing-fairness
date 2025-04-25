package com.event.processing.notifier.producer;

import com.event.processing.notifier.domain.dto.DeadLetterQueueEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;

/**
 * Interface defining the contract for publishing failed events to a dead letter queue.
 * This interface provides a standardized way to handle events that could not be processed
 * successfully, allowing for later analysis and potential reprocessing.
 *
 * @author LongLe
 * @version 2.0
 */
public interface DeadLetterQueueProducer {
  /**
   * Publishes a failed event to the dead letter queue.
   *
   * @param topic   The dead letter queue topic to publish the event to
   * @param key     The key for the Kafka record
   * @param payload The dead letter queue event payload containing failure information
   */
  void publish(String topic, String key, DeadLetterQueueEventDTO payload);

  /**
   * Publishes a failed webhook event to the dead letter queue with a specific failure reason.
   * This method converts the webhook event to a dead letter queue event format before publishing.
   *
   * @param topic         The dead letter queue topic to publish the event to
   * @param key           The key for the Kafka record
   * @param payload       The webhook event payload that failed processing
   * @param failureReason The reason why the event processing failed
   */
  void publishWithFailureReason(String topic, String key, WebhookEventDTO payload, String failureReason);
}
