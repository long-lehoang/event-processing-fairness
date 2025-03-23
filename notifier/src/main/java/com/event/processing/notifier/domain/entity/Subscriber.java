package com.event.processing.notifier.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;


@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Subscriber extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column
  private String status;

  @Column
  private String source;

  @Column
  private String firstName;

  @Column
  private String lastName;

  @Column
  private String customFields;

  @Column
  private String optinIp;

  @Column
  private Instant optinTimestamp;
}
