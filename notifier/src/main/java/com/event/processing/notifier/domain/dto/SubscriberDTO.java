package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.Set;

/**
 * Data Transfer Object for subscriber information.
 * This class represents the complete profile of a subscriber in the system,
 * including personal information, preferences, and subscription details.
 * <p>
 * The DTO includes:
 * - Basic subscriber information (ID, name, email)
 * - Subscription status and source
 * - Segmentation information
 * - Custom fields for additional data
 * - Opt-in and creation timestamps
 * <p>
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
public class SubscriberDTO {
  /**
   * Unique identifier for the subscriber.
   */
  @JsonProperty("id")
  private String id;

  /**
   * Current subscription status of the subscriber.
   */
  @JsonProperty("status")
  private String status;

  /**
   * Email address of the subscriber.
   */
  @JsonProperty("email")
  private String email;

  /**
   * Source or origin of the subscription.
   */
  @JsonProperty("source")
  private String source;

  /**
   * First name of the subscriber.
   */
  @JsonProperty("first_name")
  private String firstName;

  /**
   * Last name of the subscriber.
   */
  @JsonProperty("last_name")
  private String lastName;

  /**
   * Set of segments the subscriber belongs to.
   */
  @JsonProperty("segments")
  private Set<SegmentDTO> segments;

  /**
   * Additional custom fields in JSON format.
   */
  @JsonProperty("custom_fields")
  private String customFields; // Json

  /**
   * IP address from which the subscriber opted in.
   */
  @JsonProperty("optin_ip")
  private String optinIp;

  /**
   * Timestamp when the subscriber opted in.
   */
  @JsonProperty("optin_timestamp")
  private String optinTimestamp;

  /**
   * Timestamp when the subscriber record was created.
   */
  @JsonProperty("created_at")
  private String createdAt;

  /**
   * Constructor that accepts Instant objects for timestamps.
   * Automatically converts Instant objects to String representations.
   *
   * @param id             Subscriber identifier
   * @param status         Subscription status
   * @param email          Email address
   * @param source         Subscription source
   * @param firstName      First name
   * @param lastName       Last name
   * @param customFields   Custom fields in JSON format
   * @param optinIp        Opt-in IP address
   * @param optinTimestamp Opt-in timestamp
   * @param createdAt      Creation timestamp
   */
  public SubscriberDTO(String id, String status, String email, String source,
                       String firstName, String lastName, String customFields,
                       String optinIp, Instant optinTimestamp, Instant createdAt) {
    this.id = id;
    this.status = status;
    this.email = email;
    this.source = source;
    this.firstName = firstName;
    this.lastName = lastName;
    this.customFields = customFields;
    this.optinIp = optinIp;
    this.optinTimestamp = String.valueOf(optinTimestamp);
    this.createdAt = String.valueOf(createdAt);
  }
}
