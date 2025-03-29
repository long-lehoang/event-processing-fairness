package com.event.processing.notifier.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing the relationship between webhooks and events.
 * This class manages which events should be sent to which webhook endpoints.
 * <p>
 * Key features:
 * - Unique relationship identification
 * - Webhook reference
 * - Event reference
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
public class WebhookEvent extends BaseEntity {
  /**
   * Unique identifier for the webhook-event relationship.
   * Generated using UUID strategy.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  /**
   * Identifier of the associated webhook.
   */
  @Column
  private String webhookId;

  /**
   * Identifier of the associated event.
   */
  @Column
  private String eventId;
}
