package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@Builder
@EqualsAndHashCode
public class SubscriberDTO {
  @JsonProperty("id")
  private String id;
  @JsonProperty("status")
  private String status;
  @JsonProperty("email")
  private String email;
  @JsonProperty("source")
  private String source;
  @JsonProperty("first_name")
  private String firstName;
  @JsonProperty("last_name")
  private String lastName;
  @JsonProperty("segments")
  private Set<SegmentDTO> segments;
  @JsonProperty("custom_fields")
  private Object customFields; // Json
  @JsonProperty("optin_ip")
  private String optinIp;
  @JsonProperty("optin_timestamp")
  private String optinTimestamp;
  @JsonProperty("created_at")
  private String createdAt;
}
