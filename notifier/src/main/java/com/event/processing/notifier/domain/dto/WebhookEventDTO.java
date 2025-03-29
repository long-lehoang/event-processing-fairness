package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for webhook events.
 * This class represents the structure of webhook events received from external
 * systems.
 * <p>
 * The DTO includes:
 * - Event identification
 * - Event type classification
 * - Associated account information
 * <p>
 * Uses Lombok annotations for reducing boilerplate code and Jackson annotations
 * for JSON property mapping.
 *
 * @author LongLe
 * @version 1.0
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class WebhookEventDTO {
  /**
   * Unique identifier for the event.
   * Mapped to 'event_id' in JSON.
   */
  @JsonProperty("event_id")
  private String eventId;

  /**
   * Type or category of the event.
   * Mapped to 'event_type' in JSON.
   */
  @JsonProperty("event_type")
  private String eventType;

  /**
   * Identifier of the account associated with the event.
   * Mapped to 'account_id' in JSON.
   */
  @JsonProperty("account_id")
  private String accountId;
}
