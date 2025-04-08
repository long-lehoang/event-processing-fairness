package com.event.processing.dlq_service.consumer;

import com.event.processing.dlq_service.service.DeadLetterQueueService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueueConsumer {
    private final DeadLetterQueueService dlqService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
        topics = "${spring.kafka.topic.dead-letter-queue}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            
            String eventId = eventNode.get("eventId").asText();
            String accountId = eventNode.get("accountId").asText();
            String eventType = eventNode.get("eventType").asText();
            String failureReason = eventNode.get("failureReason").asText();
            
            dlqService.handleDeadLetterEvent(
                eventId,
                accountId,
                eventType,
                message,
                failureReason
            );
            
            meterRegistry.counter("dlq.messages.processed").increment();
        } catch (Exception e) {
            log.error("Error processing DLQ message: {}", e.getMessage(), e);
            meterRegistry.counter("dlq.messages.failed").increment();
        }
    }
}