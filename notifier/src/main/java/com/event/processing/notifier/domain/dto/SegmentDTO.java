package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigInteger;

@Data
@Builder
@EqualsAndHashCode
public class SegmentDTO {
  @JsonProperty("id")
  private String id;
  @JsonProperty("name")
  private String name;
  @JsonProperty("total_active_subscribers")
  private BigInteger totalActiveSubscribers;
  @JsonProperty("created_at")
  private String createAt;
}
