package com.event.processing.notifier.domain.entity;

import jakarta.persistence.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.Instant;

@Data
public class BaseEntity {

  @Column
  private Instant createdAt;

  @Column
  private String createdBy;
}
