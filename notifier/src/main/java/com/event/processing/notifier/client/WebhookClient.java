package com.event.processing.notifier.client;

import com.event.processing.notifier.domain.dto.BaseEventDTO;

/**
 * Interface defining the contract for webhook delivery functionality.
 * This interface provides a standardized way to send webhook notifications
 * to external endpoints.
 * <p>
 * Key features:
 * - Webhook delivery to external URLs
 * - Event payload handling
 * - Success/failure reporting
 * - Generic event payload support
 *
 * @author LongLe
 * @version 1.0
 */
public interface WebhookClient {
  /**
   * Sends a webhook notification to the specified URL.
   * This method handles the HTTP request to deliver the webhook payload
   * and reports the success or failure of the delivery.
   *
   * @param url     The destination URL for the webhook notification
   * @param payload The event payload to be sent in the webhook
   * @return true if the webhook was successfully delivered, false otherwise
   */
  boolean sendWebhook(String url, BaseEventDTO payload);
}