package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

/**
 * Data Transfer Object for subscriber events.
 * This class represents events related to subscribers and their webhook
 * notifications.
 * Extends BaseEventDTO to maintain consistency in the event hierarchy.
 *
 * The DTO includes:
 * - Event identification and timing
 * - Subscriber information
 * - Webhook configuration details
 *
 * Uses Lombok annotations for reducing boilerplate code and Jackson annotations
 * for JSON property mapping.
 *
 * @author LongLe
 * @version 1.0
 */
@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class SubscriberEventDTO extends BaseEventDTO {
  /**
   * Internal identifier for the event.
   * Not exposed in JSON serialization.
   */
  @JsonIgnore
  private String id;

  /**
   * Name or type of the subscriber event.
   * Mapped to 'event_name' in JSON.
   */
  @JsonProperty("event_name")
  private String eventName;

  /**
   * Timestamp of when the event occurred.
   * Mapped to 'event_time' in JSON.
   */
  @JsonProperty("event_time")
  private String eventTime;

  /**
   * Information about the subscriber.
   * Mapped to 'subscriber' in JSON.
   */
  @JsonProperty("subscriber")
  private SubscriberDTO subscriber;

  /**
   * Identifier of the associated webhook.
   * Mapped to 'webhook_id' in JSON.
   */
  @JsonProperty("webhook_id")
  private String webhookId;

  /**
   * Constructor that accepts an Instant for event time.
   * Automatically converts the Instant to a String representation.
   *
   * @param id         Event identifier
   * @param eventName  Name of the event
   * @param eventTime  Timestamp of the event
   * @param subscriber Subscriber information
   * @param webhookId  Webhook identifier
   */
  public SubscriberEventDTO(String id, String eventName, Instant eventTime, SubscriberDTO subscriber,
      String webhookId) {
    this.id = id;
    this.eventName = eventName;
    this.eventTime = String.valueOf(eventTime);
    this.subscriber = subscriber;
    this.webhookId = webhookId;
  }
}
