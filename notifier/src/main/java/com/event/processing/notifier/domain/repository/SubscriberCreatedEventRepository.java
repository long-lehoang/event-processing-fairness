package com.event.processing.notifier.domain.repository;

import com.event.processing.notifier.domain.dto.SegmentDTO;
import com.event.processing.notifier.domain.dto.SubscriberEventDTO;
import com.event.processing.notifier.domain.dto.SubscriberPostUrlDTO;
import com.event.processing.notifier.domain.entity.SubscriberCreatedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface SubscriberCreatedEventRepository extends JpaRepository<SubscriberCreatedEvent, String> {
  @Query("""
        SELECT new com.event.processing.notifier.domain.dto.SubscriberPostUrlDTO(s.id, w.postUrl)
        FROM SubscriberCreatedEvent s
        JOIN Webhook w ON s.webhookId = w.id
        WHERE s.id IN :eventIds
      """)
  List<SubscriberPostUrlDTO> findPostUrlsByEventIds(Set<String> eventIds);

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
