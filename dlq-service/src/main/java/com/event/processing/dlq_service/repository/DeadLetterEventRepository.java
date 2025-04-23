package com.event.processing.dlq_service.repository;

import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repository for DeadLetterEvent entities.
 * Provides methods for querying and managing dead letter events.
 */
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, String> {

  /**
   * Saves all given entities in a batch operation.
   * This method is more efficient than saving entities one by one.
   *
   * @param entities The entities to save
   * @return The saved entities
   */
  @Override
  <S extends DeadLetterEvent> List<S> saveAll(Iterable<S> entities);

  /**
   * Finds events with the given status and next retry time before the specified time.
   *
   * @param status The status to filter by
   * @param time   The time to compare against nextRetryAt
   * @return A list of matching events
   */
  List<DeadLetterEvent> findByStatusAndNextRetryAtBefore(String status, Instant time);

  /**
   * Finds events with the given status and next retry time before the specified time with pagination.
   *
   * @param status   The status to filter by
   * @param time     The time to compare against nextRetryAt
   * @param pageable Pagination information
   * @return A page of matching events
   */
  Page<DeadLetterEvent> findByStatusAndNextRetryAtBefore(String status, Instant time, Pageable pageable);

  /**
   * Finds events that are ready for retry based on status, retry count, and next retry time.
   *
   * @param maxRetries The maximum number of retries
   * @param now        The current time
   * @return A list of events ready for retry
   */
  @Query("SELECT e FROM DeadLetterEvent e WHERE e.status = 'PENDING' AND e.retryCount < :maxRetries AND e.nextRetryAt <= :now")
  List<DeadLetterEvent> findEventsForRetry(@Param("maxRetries") int maxRetries, @Param("now") Instant now);

  /**
   * Finds events that are ready for retry based on status, retry count, and next retry time with pagination.
   *
   * @param maxRetries The maximum number of retries
   * @param now        The current time
   * @param pageable   Pagination information
   * @return A page of events ready for retry
   */
  @Query("SELECT e FROM DeadLetterEvent e WHERE e.status = 'PENDING' AND e.retryCount < :maxRetries AND e.nextRetryAt <= :now")
  Page<DeadLetterEvent> findEventsForRetryPaged(@Param("maxRetries") int maxRetries, @Param("now") Instant now, Pageable pageable);
}