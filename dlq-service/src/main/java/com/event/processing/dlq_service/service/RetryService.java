package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for retrying dead letter events.
 * Following the Single Responsibility Principle, this interface focuses solely on
 * the retry functionality of dead letter events.
 */
public interface RetryService {

  /**
   * Retries a single dead letter event.
   *
   * @param event The event to retry
   * @return A CompletableFuture that completes when the retry is done
   */
  CompletableFuture<Void> retryEvent(DeadLetterEvent event);

  /**
   * Processes all events that are ready for retry.
   * This method is typically called on a schedule.
   */
  void processRetries();

  /**
   * Processes events that are ready for retry with the given status.
   *
   * @param status The status of events to retry
   * @param now    The current time
   * @return A list of event IDs that were processed for retry
   */
  List<String> processRetries(String status, Instant now);

  /**
   * Processes events that are ready for retry with the given status using multiple threads.
   *
   * @param status           The status of events to retry
   * @param now              The current time
   * @param concurrencyLevel The number of threads to use for processing
   * @return A list of event IDs that were processed for retry
   */
  List<String> processRetriesConcurrently(String status, Instant now, int concurrencyLevel);

  /**
   * Processes events that are ready for retry with the given status using multiple threads
   * and a custom batch size.
   *
   * @param status           The status of events to retry
   * @param now              The current time
   * @param concurrencyLevel The number of threads to use for processing
   * @param batchSize        The size of each batch to process
   * @return A list of event IDs that were processed for retry
   */
  List<String> processRetriesConcurrently(String status, Instant now, int concurrencyLevel, int batchSize);
}
