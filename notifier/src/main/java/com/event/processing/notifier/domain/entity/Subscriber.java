package com.event.processing.notifier.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * Entity class representing a subscriber in the system.
 * This class defines the core subscriber information and preferences,
 * including personal details and subscription status.
 * <p>
 * Key features:
 * - Unique subscriber identification
 * - Required and unique email address
 * - Subscription status tracking
 * - Personal information
 * - Custom fields support
 * - Opt-in tracking
 * - Audit information inheritance
 * <p>
 * Uses JPA annotations for database mapping and Lombok annotations
 * for reducing boilerplate code.
 *
 * @author LongLe
 * @version 1.0
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Subscriber extends BaseEntity {

  /**
   * Unique identifier for the subscriber.
   * Generated using UUID strategy.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  /**
   * Email address of the subscriber.
   * Required field and must be unique across all subscribers.
   */
  @Column(nullable = false, unique = true)
  private String email;

  /**
   * Current subscription status of the subscriber.
   */
  @Column
  private String status;

  /**
   * Source or origin of the subscription.
   */
  @Column
  private String source;

  /**
   * First name of the subscriber.
   */
  @Column
  private String firstName;

  /**
   * Last name of the subscriber.
   */
  @Column
  private String lastName;

  /**
   * Additional custom fields in JSON format.
   * Allows for flexible storage of subscriber-specific data.
   */
  @Column
  private String customFields;

  /**
   * IP address from which the subscriber opted in.
   */
  @Column
  private String optinIp;

  /**
   * Timestamp when the subscriber opted in.
   */
  @Column
  private Instant optinTimestamp;
}
