package com.event.processing.notifier.producer;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;

/**
 * Interface defining the contract for publishing webhook events to a message
 * broker.
 * This interface provides a standardized way to publish webhook events to a
 * specified topic
 * with a unique identifier.
 * <p>
 * Key features:
 * - Topic-based event publishing
 * - Unique event identification
 * - Webhook event payload handling
 *
 * @author LongLe
 * @version 1.0
 */
public interface EventProducer {
  /**
   * Publishes a webhook event to the specified topic.
   *
   * @param topic   The topic to publish the event to
   * @param id      The unique identifier for the event
   * @param payload The webhook event payload to publish
   */
  void publish(String topic, String id, WebhookEventDTO payload);
}
