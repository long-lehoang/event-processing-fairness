package com.event.processing.notifier.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.time.Instant;

@Data
@MappedSuperclass
public class BaseEntity {

  @Column
  private Instant createdAt;

  @Column
  private String createdBy;
}
