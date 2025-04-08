package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import com.event.processing.dlq_service.repository.DeadLetterEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {
    private final DeadLetterEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${dlq.retry.max-attempts}")
    private int maxRetryAttempts;

    @Value("${dlq.retry.initial-delay}")
    private long initialDelaySeconds;

    @Value("${dlq.retry.multiplier}")
    private double multiplier;

    @Value("${spring.kafka.topic.webhook-events}")
    private String webhookEventsTopic;

    public void handleDeadLetterEvent(String eventId, String accountId, String eventType, 
                                    String payload, String failureReason) {
        meterRegistry.counter("dlq.events.received").increment();
        
        DeadLetterEvent existingEvent = repository.findById(eventId).orElse(null);
        
        if (existingEvent != null) {
            updateExistingEvent(existingEvent, failureReason);
        } else {
            createNewEvent(eventId, accountId, eventType, payload, failureReason);
        }
    }

    private void updateExistingEvent(DeadLetterEvent event, String failureReason) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastErrorMessage(failureReason);
        event.setLastRetryAt(Instant.now());
        event.setNextRetryAt(calculateNextRetryTime(event.getRetryCount()));
        
        if (event.getRetryCount() >= maxRetryAttempts) {
            event.setStatus("FAILED");
            meterRegistry.counter("dlq.events.max_retries_exceeded").increment();
            log.error("Max retries exceeded for event: {}", event.getEventId());
        }
        
        repository.save(event);
    }

    private void createNewEvent(String eventId, String accountId, String eventType, 
                              String payload, String failureReason) {
        DeadLetterEvent event = DeadLetterEvent.builder()
            .eventId(eventId)
            .accountId(accountId)
            .eventType(eventType)
            .payload(payload)
            .retryCount(0)
            .status("PENDING")
            .createdAt(Instant.now())
            .lastErrorMessage(failureReason)
            .failureReason(failureReason)
            .nextRetryAt(calculateNextRetryTime(0))
            .build();
            
        repository.save(event);
        log.info("Created new DLQ event: {}", eventId);
        meterRegistry.counter("dlq.events.created").increment();
    }

    private Instant calculateNextRetryTime(int retryCount) {
        long delaySeconds = (long) (initialDelaySeconds * Math.pow(multiplier, retryCount));
        return Instant.now().plusSeconds(delaySeconds);
    }

    @Scheduled(fixedDelayString = "${dlq.retry.check-interval:60000}")
    @Transactional
    public void processRetries() {
        List<DeadLetterEvent> eventsToRetry = repository.findEventsForRetry(maxRetryAttempts, Instant.now());
        
        log.info("Processing {} events for retry", eventsToRetry.size());
        meterRegistry.gauge("dlq.events.pending_retries", eventsToRetry.size());

        for (DeadLetterEvent event : eventsToRetry) {
            retryEvent(event);
        }
    }

    private void retryEvent(DeadLetterEvent event) {
        try {
            kafkaTemplate.send(webhookEventsTopic, event.getEventId(), event.getPayload());
            event.setStatus("RETRYING");
            event.setLastRetryAt(Instant.now());
            repository.save(event);
            
            log.info("Retried event: {}, retry count: {}", event.getEventId(), event.getRetryCount());
            meterRegistry.counter("dlq.events.retried").increment();
        } catch (Exception e) {
            log.error("Failed to retry event: {}", event.getEventId(), e);
            meterRegistry.counter("dlq.events.retry_failed").increment();
            updateExistingEvent(event, e.getMessage());
        }
    }
}