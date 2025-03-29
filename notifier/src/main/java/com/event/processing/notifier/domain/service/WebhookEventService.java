package com.event.processing.notifier.domain.service;

import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.service.provider.WebhookEventProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service class responsible for managing webhook event processing.
 * This class coordinates with various event providers to retrieve webhook URLs
 * and event payloads based on event type and IDs.
 * <p>
 * Key features:
 * - Dynamic event provider selection
 * - Webhook URL retrieval
 * - Event payload retrieval
 * - Event type validation
 *
 * @author LongLe
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class WebhookEventService {

  /**
   * List of webhook event providers, each handling specific event types.
   * Providers are automatically injected and used based on event type support.
   */
  private final List<WebhookEventProvider> providers;

  /**
   * Retrieves webhook URLs for a set of events of a specific type.
   * Delegates to the appropriate provider based on event type.
   *
   * @param eventType The type of events to get webhook URLs for
   * @param eventIds  Set of event IDs to get webhook URLs for
   * @return Map of event IDs to their corresponding webhook URLs
   * @throws IllegalArgumentException if the event type is not supported by any
   *                                  provider
   */
  public Map<String, String> getWebhookUrls(String eventType, Set<String> eventIds) {
    return providers.stream()
        .filter(provider -> provider.supports(eventType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported event type: " + eventType))
        .getWebhookUrls(eventIds);
  }

  /**
   * Retrieves event payloads for a set of events of a specific type.
   * Delegates to the appropriate provider based on event type.
   *
   * @param eventType The type of events to get payloads for
   * @param eventIds  Set of event IDs to get payloads for
   * @return Map of event IDs to their corresponding event payloads
   * @throws IllegalArgumentException if the event type is not supported by any
   *                                  provider
   */
  public Map<String, BaseEventDTO> getPayloads(String eventType, Set<String> eventIds) {
    return providers.stream()
        .filter(provider -> provider.supports(eventType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported event type: " + eventType))
        .getPayloads(eventIds);
  }
}
