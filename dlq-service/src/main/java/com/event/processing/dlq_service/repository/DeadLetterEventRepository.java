package com.event.processing.dlq_service.repository;

import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, String> {
    List<DeadLetterEvent> findByStatusAndNextRetryAtBefore(String status, Instant time);
    
    @Query("SELECT e FROM DeadLetterEvent e WHERE e.status = 'PENDING' AND e.retryCount < :maxRetries AND e.nextRetryAt <= :now")
    List<DeadLetterEvent> findEventsForRetry(int maxRetries, Instant now);
}