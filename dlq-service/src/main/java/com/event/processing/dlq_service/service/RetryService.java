package com.event.processing.dlq_service.service;

import java.time.Instant;
import java.util.List;

/**
 * Service responsible for retrying dead letter events.
 * Following the Single Responsibility Principle, this interface focuses solely on
 * the retry functionality of dead letter events.
 */
public interface RetryService {

  /**
   * Processes events that are ready for retry with the given status.
   *
   * @param status The status of events to retry
   * @param now    The current time
   * @return A list of event IDs that were processed for retry
   */
  List<String> processRetries(String status, Instant now);
}
