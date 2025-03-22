package com.event.processing.notifier.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Entity
@Data
@EqualsAndHashCode
public class SubscriberCreatedEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column
  private String eventName;

  @Column
  private Instant eventTime;

  @Column
  private String subscriberId;

  @Column
  private String webhookId;
}
