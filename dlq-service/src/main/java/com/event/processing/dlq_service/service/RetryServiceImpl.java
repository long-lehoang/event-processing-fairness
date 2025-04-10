package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.constants.EventStatusConstants;
import com.event.processing.dlq_service.constants.MetricConstants;
import com.event.processing.dlq_service.domain.dto.WebhookEventDTO;
import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import com.event.processing.dlq_service.domain.mapper.DeadLetterQueueMapper;
import com.event.processing.dlq_service.producer.EventProducer;
import com.event.processing.dlq_service.repository.DeadLetterEventRepository;
import com.event.processing.dlq_service.util.PaginationUtils;
import com.event.processing.dlq_service.util.RetryUtils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Implementation of the RetryService interface.
 * This class is responsible for retrying dead letter events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryServiceImpl implements RetryService {

  private final DeadLetterEventRepository repository;
  private final EventProducer<WebhookEventDTO> webhookEventProducer;
  private final DeadLetterQueueMapper mapper;
  private final MeterRegistry meterRegistry;
  private final Executor retryTaskExecutor;

  @Value("${spring.kafka.topic.webhook-events}")
  private String webhookEventsTopic;

  @Value("${dlq.retry.max-attempts:5}")
  private int maxRetryAttempts;

  @Value("${dlq.retry.initial-delay:300}")
  private long initialDelaySeconds;

  @Value("${dlq.retry.multiplier:2.0}")
  private double multiplier;

  @Value("${dlq.retry.batch-size:100}")
  private int batchSize;

  @Value("${dlq.retry.concurrency:4}")
  private int defaultConcurrencyLevel;

  /**
   * {@inheritDoc}
   */
  @Override
  @Async("retryTaskExecutor")
  public CompletableFuture<Void> retryEvent(DeadLetterEvent event) {
    try {
      // Convert the stored event to a WebhookEventDTO
      WebhookEventDTO webhookEvent = mapper.toWebhookEventDTO(event);

      // Publish the event to the webhook events topic
      webhookEventProducer.publishEvent(webhookEventsTopic, webhookEvent);

      // Update the event status
      event.setStatus(EventStatusConstants.RETRYING);
      event.setLastRetryAt(Instant.now());
      event.setNextRetryAt(RetryUtils.calculateNextRetryTime(event.getRetryCount(), initialDelaySeconds, multiplier));
      repository.save(event);

      log.info("Retried event: {}, retry count: {}", event.getEventId(), event.getRetryCount());
      meterRegistry.counter(MetricConstants.DLQ_EVENTS_RETRIED).increment();

      return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
      log.error("Failed to retry event: {}", event.getEventId(), e);
      meterRegistry.counter(MetricConstants.DLQ_EVENTS_RETRY_FAILED).increment();

      // Update the event with the failure
      event.setRetryCount(event.getRetryCount() + 1);
      event.setLastErrorMessage(e.getMessage());
      event.setLastRetryAt(Instant.now());
      event.setNextRetryAt(RetryUtils.calculateNextRetryTime(event.getRetryCount(), initialDelaySeconds, multiplier));

      if (event.getRetryCount() >= maxRetryAttempts) {
        event.setStatus(EventStatusConstants.FAILED);
        meterRegistry.counter(MetricConstants.DLQ_EVENTS_MAX_RETRIES_EXCEEDED).increment();
        log.error("Max retries exceeded for event: {}", event.getEventId());
      }

      repository.save(event);

      return CompletableFuture.failedFuture(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Scheduled(fixedDelayString = "${dlq.retry.check-interval:60000}")
  @Transactional(readOnly = true)
  public void processRetries() {
    processRetriesConcurrently("PENDING", Instant.now(), defaultConcurrencyLevel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(readOnly = true)
  public List<String> processRetries(String status, Instant now) {
    log.info("Starting to process retries with status: {} and time before: {}", status, now);

    // Process events in batches and collect results
    List<String> processedEventIds = processEventsInBatches(status, now);

    log.info("Completed processing {} events for retry", processedEventIds.size());
    return processedEventIds;
  }

  /**
   * Processes events in batches and collects the results.
   *
   * @param status The status of events to process
   * @param now    The current time
   * @return A list of processed event IDs
   */
  private List<String> processEventsInBatches(String status, Instant now) {
    return PaginationUtils.processPaginated(
        batchSize,
        pageable -> repository.findByStatusAndNextRetryAtBeforePaged(status, now, pageable),
        this::processAndReturnEventId
    );
  }

  /**
   * Processes a single event and returns its ID if successful.
   *
   * @param event The event to process
   * @return The event ID if successful, null otherwise
   */
  private String processAndReturnEventId(DeadLetterEvent event) {
    try {
      retryEvent(event).join(); // Wait for completion
      return event.getEventId();
    } catch (Exception e) {
      log.error("Error processing retry for event: {}", event.getEventId(), e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(readOnly = true)
  public List<String> processRetriesConcurrently(String status, Instant now, int concurrencyLevel) {
    return processRetriesConcurrently(status, now, concurrencyLevel, this.batchSize);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(readOnly = true)
  public List<String> processRetriesConcurrently(String status, Instant now, int concurrencyLevel, int customBatchSize) {
    log.info("Starting to process retries concurrently with status: {}, time before: {}, concurrency level: {}",
        status, now, concurrencyLevel);

    List<CompletableFuture<String>> futures = new ArrayList<>();

    // Process events in batches using pagination
    processEventBatchesWithPagination(status, now, retryTaskExecutor, futures, this.batchSize);

    // Wait for all futures to complete
    CompletableFuture<Void> allFutures = CompletableFuture.allOf(
        futures.toArray(new CompletableFuture[0]));

    // Get the results
    allFutures.join();
    List<String> processedEventIds = futures.stream()
        .map(CompletableFuture::join)
        .filter(id -> id != null)
        .collect(Collectors.toList());

    log.info("Completed processing {} events for retry concurrently", processedEventIds.size());
    meterRegistry.gauge(MetricConstants.DLQ_EVENTS_PENDING_RETRIES, processedEventIds.size());

    return processedEventIds;
  }

  /**
   * Processes event batches using pagination.
   * This method fetches events in batches and processes them concurrently.
   *
   * @param status   The status of events to process
   * @param now      The current time
   * @param executor The executor service for concurrent processing
   * @param futures  The list to collect futures for tracking completion
   */
  private void processEventBatchesWithPagination(String status, Instant now, Executor executor,
                                                 List<CompletableFuture<String>> futures, int batchSize) {
    int pageNumber = 0;
    boolean hasMorePages = true;

    while (hasMorePages) {
      Pageable pageable = PageRequest.of(pageNumber, batchSize);
      Page<DeadLetterEvent> eventsPage = repository.findByStatusAndNextRetryAtBeforePaged(status, now, pageable);

      List<DeadLetterEvent> events = eventsPage.getContent();
      if (events.isEmpty()) {
        hasMorePages = false;
        continue;
      }

      log.info("Processing batch of {} events for retry concurrently (page {})", events.size(), pageNumber);

      // Create a batch future for processing all events in this batch
      CompletableFuture<List<String>> batchFuture = createBatchRetryFuture(events, executor);

      // When the batch is complete, add individual results to the futures list
      batchFuture.thenAccept(eventIds -> {
        synchronized (futures) {
          for (String eventId : eventIds) {
            if (eventId != null) {
              CompletableFuture<String> future = CompletableFuture.completedFuture(eventId);
              futures.add(future);
            }
          }
        }
      });

      pageNumber++;
      hasMorePages = eventsPage.hasNext();
    }
  }

  /**
   * Creates a CompletableFuture for retrying a batch of events.
   * This method processes events in a batch for better performance.
   * Uses a transaction to ensure database consistency.
   *
   * @param events   The list of events to retry
   * @param executor The executor service for concurrent processing
   * @return A CompletableFuture that will complete with a list of event IDs
   */
  @Transactional
  private CompletableFuture<List<String>> createBatchRetryFuture(List<DeadLetterEvent> events, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Prepare for batch processing
        List<String> successfulEventIds = new ArrayList<>();
        List<DeadLetterEvent> eventsToUpdate = new ArrayList<>();

        // First pass: publish all events to Kafka
        for (DeadLetterEvent event : events) {
          try {
            // Convert the stored event to a WebhookEventDTO
            WebhookEventDTO webhookEvent = mapper.toWebhookEventDTO(event);

            // Publish the event to the webhook events topic
            webhookEventProducer.publishEvent(webhookEventsTopic, webhookEvent);

            // Update the event status
            event.setStatus(EventStatusConstants.RETRYING);
            event.setLastRetryAt(Instant.now());
            event.setNextRetryAt(RetryUtils.calculateNextRetryTime(event.getRetryCount(), initialDelaySeconds, multiplier));

            // Add to the list of events to update
            eventsToUpdate.add(event);
            successfulEventIds.add(event.getEventId());

            log.debug("Prepared event for retry: {}", event.getEventId());
          } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getEventId(), e);
            meterRegistry.counter(MetricConstants.DLQ_EVENTS_RETRY_FAILED).increment();

            // Update the event with the failure
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastErrorMessage(e.getMessage());
            event.setLastRetryAt(Instant.now());
            event.setNextRetryAt(RetryUtils.calculateNextRetryTime(event.getRetryCount(), initialDelaySeconds, multiplier));

            if (event.getRetryCount() >= maxRetryAttempts) {
              event.setStatus(EventStatusConstants.FAILED);
              meterRegistry.counter(MetricConstants.DLQ_EVENTS_MAX_RETRIES_EXCEEDED).increment();
              log.error("Max retries exceeded for event: {}", event.getEventId());
            }

            // Add to the list of events to update
            eventsToUpdate.add(event);
          }
        }

        // Second pass: batch update all events in the database
        if (!eventsToUpdate.isEmpty()) {
          log.info("Batch updating {} events after retry attempt", eventsToUpdate.size());
          repository.saveAll(eventsToUpdate);
          meterRegistry.counter(MetricConstants.DLQ_EVENTS_RETRIED).increment(successfulEventIds.size());
        }

        return successfulEventIds;
      } catch (Exception e) {
        log.error("Error in batch retry processing", e);
        return new ArrayList<>();
      }
    }, executor);
  }

  /**
   * Creates a CompletableFuture for retrying a single event.
   * This method is kept for backward compatibility.
   *
   * @param event    The event to retry
   * @param executor The executor service for concurrent processing
   * @return A CompletableFuture that will complete with the event ID or null on failure
   */
  private CompletableFuture<String> createRetryFuture(DeadLetterEvent event, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        retryEvent(event).join();
        return event.getEventId();
      } catch (Exception e) {
        log.error("Error processing retry for event: {}", event.getEventId(), e);
        return null;
      }
    }, executor);
  }
}
