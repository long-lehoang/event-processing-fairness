package com.event.processing.notifier.domain.repository;

import com.event.processing.notifier.domain.dto.SubscriberEventDTO;
import com.event.processing.notifier.domain.dto.SubscriberPostUrlDTO;
import com.event.processing.notifier.domain.entity.SubscriberCreatedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Repository interface for managing SubscriberCreatedEvent entities.
 * Provides database operations for subscriber creation events and their
 * relationships with webhooks and subscribers.
 *
 * Key features:
 * - Basic CRUD operations inherited from JpaRepository
 * - Custom queries for fetching event data with related entities
 * - DTO-based event retrieval
 * - Webhook URL lookup functionality
 *
 * @author LongLe
 * @version 1.0
 */
@Repository
public interface SubscriberCreatedEventRepository extends JpaRepository<SubscriberCreatedEvent, String> {
  /**
   * Fetches webhook post URLs for the given event IDs.
   * Uses a JPQL query to join the SubscriberCreatedEvent and Webhook tables
   * and returns the results as SubscriberPostUrlDTO objects.
   *
   * @param eventIds Set of event IDs to fetch webhook URLs for
   * @return List of SubscriberPostUrlDTO objects containing event IDs and webhook
   *         URLs
   */
  @Query("""
        SELECT new com.event.processing.notifier.domain.dto.SubscriberPostUrlDTO(s.id, w.postUrl)
        FROM SubscriberCreatedEvent s
        JOIN Webhook w ON s.webhookId = w.id
        WHERE s.id IN :eventIds
      """)
  List<SubscriberPostUrlDTO> findPostUrlsByEventIds(Set<String> eventIds);

  /**
   * Fetches subscriber events with their associated subscriber information.
   * Uses a JPQL query to join the SubscriberCreatedEvent and Subscriber tables
   * and returns the results as SubscriberEventDTO objects.
   *
   * @param eventIds Set of event IDs to fetch events for
   * @return List of SubscriberEventDTO objects containing event and subscriber
   *         information
   */
  @Query("""
        SELECT new com.event.processing.notifier.domain.dto.SubscriberEventDTO(
          s.id,
          s.eventName,
          s.eventTime,
          new com.event.processing.notifier.domain.dto.SubscriberDTO(
            sub.id,
            sub.status,
            sub.email,
            sub.source,
            sub.firstName,
            sub.lastName,
            sub.customFields,
            sub.optinIp,
            sub.optinTimestamp,
            sub.createdAt
          ),
          s.webhookId
        )
        FROM SubscriberCreatedEvent s
        JOIN Subscriber sub ON s.subscriberId = sub.id
        WHERE s.id IN :eventIds
      """)
  List<SubscriberEventDTO> fetchEventsWithoutSegments(Set<String> eventIds);
}
