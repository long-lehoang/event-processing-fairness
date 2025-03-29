package com.event.processing.notifier.service;

import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;

/**
 * Interface defining the contract for webhook notification processing.
 * This interface provides a standardized way to process webhook notifications
 * with retry capabilities for improved reliability.
 * <p>
 * Key features:
 * - Retry mechanism for failed webhook deliveries
 * - Event payload processing
 * - Webhook URL handling
 * - Event identification
 *
 * @author LongLe
 * @version 1.0
 */
public interface WebhookService {
  /**
   * Processes a webhook notification with retry capabilities.
   * This method handles the delivery of webhook notifications to the specified
   * URL,
   * implementing retry logic for failed attempts.
   *
   * @param eventId      The unique identifier of the event being processed
   * @param eventPayload The webhook event payload containing event details
   * @param url          The destination URL for the webhook notification
   * @param payload      The base event payload to be sent in the webhook
   */
  void processWithRetry(String eventId, WebhookEventDTO eventPayload, String url, BaseEventDTO payload);
}
