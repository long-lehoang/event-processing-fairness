package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.domain.dto.DeadLetterQueueEventDTO;

import java.time.Instant;
import java.util.List;

/**
 * Interface defining the contract for dead letter queue event processing.
 * Following the Interface Segregation Principle, this interface provides
 * methods for handling dead letter queue events.
 */
public interface DeadLetterQueueEventProcessor {

  /**
   * Handles a dead letter event by either creating a new entry or updating an existing one.
   *
   * @param eventId       The ID of the event
   * @param accountId     The account ID associated with the event
   * @param eventType     The type of the event
   * @param payload       The event payload
   * @param failureReason The reason for the failure
   */
  void handleDeadLetterEvent(String eventId, String accountId, String eventType, String payload, String failureReason);

  /**
   * Handles a batch of dead letter queue events.
   * This method processes multiple events in a single transaction for better performance.
   *
   * @param events The list of dead letter queue events to process
   */
  void handleDeadLetterEvents(List<DeadLetterQueueEventDTO> events);

  /**
   * Processes events that are ready for retry.
   * This method is typically called on a schedule.
   */
  void processRetries();

  /**
   * Finds events that are ready for retry based on status and time.
   *
   * @param status The status of events to find
   * @param now    The current time
   * @return A list of event IDs that are ready for retry
   */
  List<String> findEventsForRetry(String status, Instant now);
}
