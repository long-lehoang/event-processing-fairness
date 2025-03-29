package com.event.processing.notifier.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing the relationship between subscribers and segments.
 * This class manages the many-to-many relationship between subscribers
 * and their associated segments.
 * <p>
 * Key features:
 * - Unique relationship identification
 * - Segment reference
 * - Subscriber reference
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
public class SubscriberSegment extends BaseEntity {
  /**
   * Unique identifier for the subscriber-segment relationship.
   * Generated using UUID strategy.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  /**
   * Identifier of the associated segment.
   */
  @Column
  private String segmentId;

  /**
   * Identifier of the associated subscriber.
   */
  @Column
  private String subscriberId;
}
