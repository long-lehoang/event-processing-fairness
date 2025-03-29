package com.event.processing.notifier.domain.dto;

/**
 * Record class for subscriber webhook post URL information.
 * This record represents the mapping between an event and its corresponding
 * webhook post URL for subscriber notifications.
 * <p>
 * The record includes:
 * - Event identifier
 * - Webhook post URL
 * <p>
 * Uses Java record feature for immutable data transfer.
 *
 * @author LongLe
 * @version 1.0
 */
public record SubscriberPostUrlDTO(
    /**
     * Identifier of the event to be posted.
     */
    String eventId,

    /**
     * URL where the event should be posted.
     */
    String postUrl) {
}
