package com.event.processing.notifier.domain.service.provider;

import com.event.processing.notifier.domain.dto.BaseEventDTO;
import com.event.processing.notifier.domain.dto.SegmentDTO;
import com.event.processing.notifier.domain.dto.SubscriberEventDTO;
import com.event.processing.notifier.domain.dto.SubscriberPostUrlDTO;
import com.event.processing.notifier.domain.repository.SegmentRepository;
import com.event.processing.notifier.domain.repository.SubscriberCreatedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of WebhookEventProvider for subscriber creation events.
 * This class handles the processing and enrichment of subscriber creation
 * events
 * with their associated segments and webhook URLs.
 * <p>
 * Key features:
 * - Subscriber creation event type support
 * - Webhook URL mapping for events
 * - Event payload enrichment with segment information
 * - Efficient batch processing of events
 *
 * @author LongLe
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class SubscriberCreatedEventProviderImpl implements WebhookEventProvider {
  private static final String EVENT_TYPE = "subscriber.created";
  private final SubscriberCreatedEventRepository subscriberCreatedEventRepository;
  private final SegmentRepository segmentRepository;

  /**
   * Checks if this provider supports the given event type.
   *
   * @param eventType The type of event to check support for
   * @return true if the event type is "subscriber.created", false otherwise
   */
  @Override
  public boolean supports(String eventType) {
    return EVENT_TYPE.equals(eventType);
  }

  /**
   * Retrieves webhook URLs for the given event IDs.
   * Maps event IDs to their corresponding webhook URLs using the repository.
   *
   * @param eventIds Set of event IDs to get webhook URLs for
   * @return Map of event IDs to their corresponding webhook URLs
   */
  @Override
  public Map<String, String> getWebhookUrls(Set<String> eventIds) {
    return subscriberCreatedEventRepository.findPostUrlsByEventIds(eventIds)
        .stream()
        .collect(Collectors.toMap(
            SubscriberPostUrlDTO::eventId,
            SubscriberPostUrlDTO::postUrl,
            (existing, replacement) -> existing // Handles duplicate keys
        ));
  }

  /**
   * Retrieves and enriches event payloads for the given event IDs.
   * Fetches event data and enriches it with segment information.
   *
   * @param eventIds Set of event IDs to get payloads for
   * @return Map of event IDs to their corresponding enriched event payloads
   */
  @Override
  public Map<String, BaseEventDTO> getPayloads(Set<String> eventIds) {
    List<SubscriberEventDTO> events = subscriberCreatedEventRepository.fetchEventsWithoutSegments(eventIds);
    Map<String, Set<SegmentDTO>> segmentMap = fetchSegmentsForSubscribers(events);

    // Assign segments
    events.forEach(event -> event.getSubscriber()
        .setSegments(segmentMap.getOrDefault(event.getSubscriber().getId(), Collections.emptySet())));

    return events.stream()
        .collect(Collectors.toMap(SubscriberEventDTO::getId, dto -> dto, (existing, replacement) -> existing));
  }

  /**
   * Fetches segments for a list of subscribers.
   * Groups segments by subscriber ID for efficient assignment.
   *
   * @param events List of subscriber events to fetch segments for
   * @return Map of subscriber IDs to their corresponding sets of segments
   */
  private Map<String, Set<SegmentDTO>> fetchSegmentsForSubscribers(List<SubscriberEventDTO> events) {
    Set<String> subscriberIds = events.stream()
        .map(e -> e.getSubscriber().getId())
        .collect(Collectors.toSet());

    if (subscriberIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return segmentRepository.fetchSegments(subscriberIds).stream()
        .collect(Collectors.groupingBy(SegmentDTO::getSubscriberId, Collectors.toSet()));
  }
}
