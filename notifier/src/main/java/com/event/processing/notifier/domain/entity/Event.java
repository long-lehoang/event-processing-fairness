package com.event.processing.notifier.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing an event type in the system.
 * This class defines the different types of events that can be processed
 * and tracked in the application.
 * <p>
 * Key features:
 * - Unique event identification
 * - Event name with uniqueness constraint
 * - Audit information inheritance
 * <p>
 * Uses JPA annotations for database mapping and Lombok annotations
 * for reducing boilerplate code.
 *
 * @author LongLe
 * @version 1.0
 */
@Entity
@EqualsAndHashCode(callSuper = true)
@Data
public class Event extends BaseEntity {
  /**
   * Unique identifier for the event type.
   * Generated using UUID strategy.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  /**
   * Name of the event type.
   * Must be unique across all event types.
   */
  @Column(unique = true)
  private String eventName;
}
