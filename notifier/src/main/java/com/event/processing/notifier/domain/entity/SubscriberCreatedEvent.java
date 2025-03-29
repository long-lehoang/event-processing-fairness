package com.event.processing.notifier.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * Entity class representing a subscriber creation event in the system.
 * This class tracks when new subscribers are created and which webhooks
 * should be notified about these events.
 * <p>
 * Key features:
 * - Unique event identification
 * - Event name and timing
 * - Subscriber reference
 * - Webhook reference
 * <p>
 * Uses JPA annotations for database mapping and Lombok annotations
 * for reducing boilerplate code.
 *
 * @author LongLe
 * @version 1.0
 */
@Entity
@Data
@EqualsAndHashCode
public class SubscriberCreatedEvent {
  /**
   * Unique identifier for the event.
   * Generated using UUID strategy.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  /**
   * Name of the event.
   * Typically "subscriber.created" or similar.
   */
  @Column
  private String eventName;

  /**
   * Timestamp when the event occurred.
   */
  @Column
  private Instant eventTime;

  /**
   * Identifier of the newly created subscriber.
   */
  @Column
  private String subscriberId;

  /**
   * Identifier of the webhook that should receive this event.
   */
  @Column
  private String webhookId;
}
