package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.constants.EventStatusConstants;
import com.event.processing.dlq_service.constants.MetricConstants;
import com.event.processing.dlq_service.domain.dto.DeadLetterQueueEventDTO;
import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import com.event.processing.dlq_service.repository.DeadLetterEventRepository;
import com.event.processing.dlq_service.util.RetryUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling dead letter queue events.
 * This class implements the DeadLetterQueueEventProcessor interface
 * and follows the Single Responsibility Principle by focusing only on
 * dead letter queue event processing.
 * <p>
 * The retry functionality has been moved to a separate RetryService
 * following the Single Responsibility Principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService implements DeadLetterQueueEventProcessor {
  private final DeadLetterEventRepository repository;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final RetryService retryService;
  private final BatchOperationService batchOperationService;

  @Value("${dlq.retry.max-attempts}")
  private int maxRetryAttempts;

  @Value("${dlq.retry.initial-delay}")
  private long initialDelaySeconds;

  @Value("${dlq.retry.multiplier:2.0}")
  private double multiplier;

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleDeadLetterEvent(String eventId, String accountId, String eventType,
                                    String payload, String failureReason) {
    meterRegistry.counter(MetricConstants.DLQ_EVENTS_RECEIVED).increment();

    DeadLetterEvent existingEvent = repository.findById(eventId).orElse(null);

    if (existingEvent != null) {
      updateExistingEvent(existingEvent, failureReason);
    } else {
      createNewEvent(eventId, accountId, eventType, payload, failureReason);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public void handleDeadLetterEvents(List<DeadLetterQueueEventDTO> events) {
    if (events == null || events.isEmpty()) {
      log.warn("Received empty or null batch of dead letter queue events");
      return;
    }

    log.info("Processing batch of {} dead letter queue events", events.size());
    meterRegistry.counter(MetricConstants.DLQ_EVENTS_BATCH_RECEIVED).increment();
    meterRegistry.gauge(MetricConstants.DLQ_EVENTS_BATCH_SIZE, events.size());

    // Prepare for batch processing with thread-safe collections
    List<DeadLetterEvent> newEvents = Collections.synchronizedList(new ArrayList<>());
    List<DeadLetterEvent> existingEvents = Collections.synchronizedList(new ArrayList<>());
    Map<String, DeadLetterQueueEventDTO> eventDtoMap = new ConcurrentHashMap<>();

    // First pass: categorize events and fetch existing ones
    for (DeadLetterQueueEventDTO eventDto : events) {
      eventDtoMap.put(eventDto.getEventId(), eventDto);
    }

    // Fetch all existing events in a single query
    List<DeadLetterEvent> foundEvents = repository.findAllById(eventDtoMap.keySet());
    Map<String, DeadLetterEvent> existingEventMap = new ConcurrentHashMap<>();
    for (DeadLetterEvent event : foundEvents) {
      existingEventMap.put(event.getEventId(), event);
    }

    // Second pass: prepare events for batch processing
    int processedCount = 0;
    int errorCount = 0;

    for (DeadLetterQueueEventDTO eventDto : events) {
      try {
        String eventId = eventDto.getEventId();
        String accountId = eventDto.getAccountId();
        String eventType = eventDto.getEventType();
        String failureReason = eventDto.getFailureReason();

        // Convert the DTO to JSON for storage
        String payload;
        try {
          payload = objectMapper.writeValueAsString(eventDto);
        } catch (Exception e) {
          log.error("Error serializing event: {}", eventId, e);
          payload = "{\"eventId\":\"" + eventId + "\"}";
        }

        DeadLetterEvent existingEvent = existingEventMap.get(eventId);

        if (existingEvent != null) {
          // Update existing event
          existingEvent.setRetryCount(existingEvent.getRetryCount() + 1);
          existingEvent.setLastErrorMessage(failureReason);
          existingEvent.setLastRetryAt(Instant.now());
          existingEvent.setNextRetryAt(RetryUtils.calculateNextRetryTime(existingEvent.getRetryCount(), initialDelaySeconds, multiplier));

          if (existingEvent.getRetryCount() >= maxRetryAttempts) {
            existingEvent.setStatus(EventStatusConstants.FAILED);
            meterRegistry.counter(MetricConstants.DLQ_EVENTS_MAX_RETRIES_EXCEEDED).increment();
            log.debug("Max retries exceeded for event: {}", existingEvent.getEventId());
          }

          existingEvents.add(existingEvent);
        } else {
          // Create new event
          DeadLetterEvent newEvent = DeadLetterEvent.builder()
              .eventId(eventId)
              .accountId(accountId)
              .eventType(eventType)
              .payload(payload)
              .retryCount(0)
              .status(EventStatusConstants.PENDING)
              .createdAt(Instant.now())
              .lastErrorMessage(failureReason)
              .failureReason(failureReason)
              .nextRetryAt(RetryUtils.calculateNextRetryTime(0, initialDelaySeconds, multiplier))
              .build();

          newEvents.add(newEvent);
        }

        processedCount++;
      } catch (Exception e) {
        log.error("Error processing dead letter queue event: {}", eventDto.getEventId(), e);
        meterRegistry.counter(MetricConstants.DLQ_EVENTS_BATCH_PROCESSING_ERROR).increment();
        errorCount++;
      }
    }

    // Batch save new events
    if (!newEvents.isEmpty()) {
      log.info("Batch saving {} new events", newEvents.size());
      repository.saveAll(newEvents);
      meterRegistry.counter(MetricConstants.DLQ_EVENTS_CREATED).increment(newEvents.size());
    }

    // Batch update existing events
    if (!existingEvents.isEmpty()) {
      log.info("Batch updating {} existing events", existingEvents.size());
      repository.saveAll(existingEvents);
    }

    log.info("Batch processing completed. Processed: {}, Errors: {}", processedCount, errorCount);
    meterRegistry.counter(MetricConstants.DLQ_EVENTS_BATCH_PROCESSED).increment(processedCount);
    meterRegistry.counter(MetricConstants.DLQ_EVENTS_BATCH_ERRORS).increment(errorCount);
  }

  /**
   * Processes a single dead letter queue event.
   * Extracts information from the event and either updates an existing event or creates a new one.
   *
   * @param event The event to process
   * @throws Exception If there is an error processing the event
   */
  private void processDeadLetterQueueEvent(DeadLetterQueueEventDTO event) throws Exception {
    String eventId = event.getEventId();
    String accountId = event.getAccountId();
    String eventType = event.getEventType();
    String failureReason = event.getFailureReason();

    // Convert the DTO to JSON for storage
    String payload = objectMapper.writeValueAsString(event);

    DeadLetterEvent existingEvent = repository.findById(eventId).orElse(null);

    if (existingEvent != null) {
      updateExistingEvent(existingEvent, failureReason);
    } else {
      createNewEvent(eventId, accountId, eventType, payload, failureReason);
    }
  }

  /**
   * Updates an existing dead letter event with new failure information.
   *
   * @param event         The event to update
   * @param failureReason The reason for the failure
   */
  private void updateExistingEvent(DeadLetterEvent event, String failureReason) {
    event.setRetryCount(event.getRetryCount() + 1);
    event.setLastErrorMessage(failureReason);
    event.setLastRetryAt(Instant.now());
    event.setNextRetryAt(RetryUtils.calculateNextRetryTime(event.getRetryCount(), initialDelaySeconds, multiplier));

    if (event.getRetryCount() >= maxRetryAttempts) {
      event.setStatus(EventStatusConstants.FAILED);
      meterRegistry.counter(MetricConstants.DLQ_EVENTS_MAX_RETRIES_EXCEEDED).increment();
      log.error("Max retries exceeded for event: {}", event.getEventId());
    }

    repository.save(event);
  }

  /**
   * Creates a new dead letter event.
   *
   * @param eventId       The ID of the event
   * @param accountId     The account ID associated with the event
   * @param eventType     The type of the event
   * @param payload       The event payload
   * @param failureReason The reason for the failure
   */
  private void createNewEvent(String eventId, String accountId, String eventType, String payload, String failureReason) {
    DeadLetterEvent event = DeadLetterEvent.builder()
        .eventId(eventId)
        .accountId(accountId)
        .eventType(eventType)
        .payload(payload)
        .retryCount(0)
        .status(EventStatusConstants.PENDING)
        .createdAt(Instant.now())
        .lastErrorMessage(failureReason)
        .failureReason(failureReason)
        .nextRetryAt(RetryUtils.calculateNextRetryTime(0, initialDelaySeconds, multiplier))
        .build();

    repository.save(event);
    log.info("Created new DLQ event: {}", eventId);
    meterRegistry.counter(MetricConstants.DLQ_EVENTS_CREATED).increment();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(readOnly = true)
  public void processRetries() {
    // Delegate to the RetryService
    retryService.processRetries();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(readOnly = true)
  public List<String> findEventsForRetry(String status, Instant now) {
    // Delegate to the RetryService
    return retryService.processRetries(status, now);
  }
}