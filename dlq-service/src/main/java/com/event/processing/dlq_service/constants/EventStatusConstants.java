package com.event.processing.dlq_service.constants;

/**
 * Constants for event statuses used throughout the application.
 * This class centralizes all event status values to ensure consistency.
 */
public final class EventStatusConstants {

  /**
   * Status for events that are waiting to be retried.
   */
  public static final String PENDING = "PENDING";
  /**
   * Status for events that are currently being retried.
   */
  public static final String RETRYING = "RETRYING";
  /**
   * Status for events that have failed after maximum retry attempts.
   */
  public static final String FAILED = "FAILED";
  /**
   * Status for events that have been successfully processed.
   */
  public static final String PROCESSED = "PROCESSED";

  private EventStatusConstants() {
    // Private constructor to prevent instantiation
  }
}
