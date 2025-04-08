package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing an event that has been sent to the Dead Letter Queue (DLQ).
 *
 * This class is used for transferring dead letter event data between different components of the system,
 * particularly when events fail processing and need to be stored for later retry or analysis.
 * The class uses Jackson annotations for JSON serialization/deserialization with snake_case property names.
 *
 * Dead letter events typically represent messages that could not be processed successfully by the
 * normal event processing flow due to errors or exceptions. This DTO is used in the notifier service
 * to handle failed webhook events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterQueueEventDTO {
    /**
     * Unique identifier for the event that failed processing.
     * This ID can be used to track the event through the system.
     */
    @JsonProperty("event_id")
    private String eventId;

    /**
     * Identifier of the account associated with this event.
     * Used for filtering and routing events to appropriate handlers.
     */
    @JsonProperty("account_id")
    private String accountId;

    /**
     * Type of the event that failed processing.
     * This helps in categorizing events and determining appropriate retry strategies.
     */
    @JsonProperty("event_type")
    private String eventType;

    /**
     * The most recent error message encountered when processing this event.
     * Provides diagnostic information about why the event processing failed.
     */
    @JsonProperty("last_error_message")
    private String lastErrorMessage;

    /**
     * The reason why the event was sent to the dead letter queue.
     * This may contain more detailed information about the failure context
     * than the last error message alone.
     */
    @JsonProperty("failure_reason")
    private String failureReason;
}