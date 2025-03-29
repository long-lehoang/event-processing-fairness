package com.event.processing.notifier.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing a webhook configuration in the system.
 * This class defines the webhook endpoints where event notifications
 * will be sent.
 * <p>
 * Key features:
 * - Unique webhook identification
 * - Webhook name for display purposes
 * - Post URL for event delivery
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
public class Webhook extends BaseEntity {
  /**
   * Unique identifier for the webhook.
   * Generated using UUID strategy.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  /**
   * Display name for the webhook.
   * Used for identification and management purposes.
   */
  @Column
  private String name;

  /**
   * URL where event notifications will be posted.
   * Must be a valid HTTP/HTTPS endpoint.
   */
  @Column
  private String postUrl;
}
