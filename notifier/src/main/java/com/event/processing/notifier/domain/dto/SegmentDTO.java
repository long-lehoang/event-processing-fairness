package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigInteger;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class SegmentDTO {
  @JsonProperty("id")
  private String id;
  @JsonProperty("name")
  private String name;
  @JsonIgnore
  private String subscriberId;
}
