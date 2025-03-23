package com.event.processing.notifier.domain.repository;

import com.event.processing.notifier.domain.dto.SegmentDTO;
import com.event.processing.notifier.domain.entity.Segment;
import com.event.processing.notifier.domain.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, String> {
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
