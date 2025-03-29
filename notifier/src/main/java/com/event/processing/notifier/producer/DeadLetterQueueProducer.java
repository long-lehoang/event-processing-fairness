package com.event.processing.notifier.producer;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;

/**
 * Interface defining the contract for publishing failed webhook events to a
 * dead letter queue.
 * This interface provides a standardized way to handle events that could not be
 * processed
 * successfully, allowing for later analysis and potential reprocessing.
 * <p>
 * Key features:
 * - Failed event handling
 * - Dead letter queue publishing
 * - Event identification and tracking
 *
 * @author LongLe
 * @version 1.0
 */
public interface DeadLetterQueueProducer {
  /**
   * Publishes a failed webhook event to the dead letter queue.
   *
   * @param topic   The dead letter queue topic to publish the event to
   * @param id      The unique identifier for the failed event
   * @param payload The webhook event payload that failed processing
   */
  void publish(String topic, String id, WebhookEventDTO payload);
}
