package com.event.processing.notifier.domain.repository;

import com.event.processing.notifier.domain.dto.SegmentDTO;
import com.event.processing.notifier.domain.entity.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Repository interface for managing Segment entities.
 * Provides database operations for segments and their relationships with
 * subscribers.
 * <p>
 * Key features:
 * - Basic CRUD operations inherited from JpaRepository
 * - Custom query for fetching segments by subscriber IDs
 * - DTO-based segment retrieval
 *
 * @author LongLe
 * @version 1.0
 */
@Repository
public interface SegmentRepository extends JpaRepository<Segment, String> {
  /**
   * Fetches segments associated with the given subscriber IDs.
   * Uses a JPQL query to join the Segment and SubscriberSegment tables
   * and returns the results as SegmentDTO objects.
   *
   * @param subscriberIds Set of subscriber IDs to fetch segments for
   * @return List of SegmentDTO objects containing segment information
   */
  @Query("""
        SELECT new com.event.processing.notifier.domain.dto.SegmentDTO(
            seg.id, seg.name, subSeg.subscriberId
        )
        FROM SubscriberSegment subSeg
        JOIN Segment seg ON subSeg.segmentId = seg.id
        WHERE subSeg.subscriberId IN :subscriberIds
      """)
  List<SegmentDTO> fetchSegments(Set<String> subscriberIds);
}
