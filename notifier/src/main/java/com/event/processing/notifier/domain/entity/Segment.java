package com.event.processing.notifier.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing a subscriber segment in the system.
 * This class defines the different segments or groups that subscribers
 * can be categorized into.
 * <p>
 * Key features:
 * - Unique segment identification
 * - Segment name
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
public class Segment extends BaseEntity {
  /**
   * Unique identifier for the segment.
   * Generated using UUID strategy.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  /**
   * Name of the segment.
   * Used for display and identification purposes.
   */
  @Column
  private String name;
}
