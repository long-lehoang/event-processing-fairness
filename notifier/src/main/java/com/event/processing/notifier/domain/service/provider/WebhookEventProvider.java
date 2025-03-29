package com.event.processing.notifier.domain.service.provider;

import com.event.processing.notifier.domain.dto.BaseEventDTO;

import java.util.Map;
import java.util.Set;

/**
 * Interface for webhook event providers.
 * This interface defines the contract for providers that handle different types
 * of webhook events and their associated data.
 * <p>
 * Key features:
 * - Event type support checking
 * - Webhook URL mapping
 * - Event payload retrieval
 *
 * @author LongLe
 * @version 1.0
 */
public interface WebhookEventProvider {
  /**
   * Checks if this provider supports the given event type.
   *
   * @param eventType The type of event to check support for
   * @return true if this provider can handle the event type, false otherwise
   */
  boolean supports(String eventType);

  /**
   * Retrieves webhook URLs for the given event IDs.
   *
   * @param eventIds Set of event IDs to get webhook URLs for
   * @return Map of event IDs to their corresponding webhook URLs
   */
  Map<String, String> getWebhookUrls(Set<String> eventIds);

  /**
   * Retrieves event payloads for the given event IDs.
   *
   * @param eventIds Set of event IDs to get payloads for
   * @return Map of event IDs to their corresponding event payloads
   */
  Map<String, BaseEventDTO> getPayloads(Set<String> eventIds);
}
