package com.event.processing.notifier.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.time.Instant;

/**
 * Base entity class for all domain entities in the application.
 * This class provides common fields and functionality for all entities,
 * including audit information.
 *
 * Key features:
 * - Creation timestamp tracking
 * - Creator identification
 * - JPA mapping support
 *
 * Uses Lombok annotations for reducing boilerplate code and JPA annotations
 * for database mapping.
 *
 * @author LongLe
 * @version 1.0
 */
@Data
@MappedSuperclass
public class BaseEntity {

  /**
   * Timestamp when the entity was created.
   * Automatically managed by the persistence layer.
   */
  @Column
  private Instant createdAt;

  /**
   * Identifier of the user or system that created the entity.
   * Used for audit tracking.
   */
  @Column
  private String createdBy;
}
