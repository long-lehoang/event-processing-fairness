package com.event.processing.notifier.application;

import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;

/**
 * Interface defining the contract for webhook event processing.
 * This interface provides a method to process webhook events and deliver them
 * to subscribers.
 *
 * Key features:
 * - Event processing and delivery
 * - Support for different event types
 * - Webhook URL management
 * - Payload transformation
 *
 * @author LongLe
 * @version 1.0
 */
public interface WebhookEventProcessing {
  /**
   * Processes a webhook event and delivers it to the specified URL.
   * This method handles:
   * - Event validation and processing
   * - Payload transformation
   * - Webhook delivery
   * - Error handling and reporting
   *
   * @param eventId        The unique identifier of the event being processed
   * @param eventPayload   The original webhook event payload
   * @param url            The destination URL for the webhook
   * @param webhookPayload The transformed payload to be sent in the webhook
   */
  void process(String eventId, WebhookEventDTO eventPayload, String url, BaseEventDTO webhookPayload);
}
