package com.event.processing.notifier.service;

/**
 * Interface defining the contract for event deduplication functionality.
 * This interface provides a standardized way to prevent duplicate event
 * processing
 * by tracking processed events and checking for duplicates.
 *
 * Key features:
 * - Event deduplication checking
 * - Processed event tracking
 * - Event identification
 * - Duplicate prevention
 *
 * @author LongLe
 * @version 1.0
 */
public interface DeduplicationService {
  /**
   * Checks if an event has already been processed.
   * This method determines whether the event is a duplicate and should be
   * skipped.
   *
   * @param eventId The unique identifier of the event to check
   * @return true if the event is a duplicate and should be skipped, false
   *         otherwise
   */
  boolean isDuplicate(String eventId);

  /**
   * Marks an event as processed to prevent future duplicate processing.
   * This method should be called after successful event processing to record
   * the event's completion.
   *
   * @param eventId The unique identifier of the processed event
   */
  void markProcessed(String eventId);
}
