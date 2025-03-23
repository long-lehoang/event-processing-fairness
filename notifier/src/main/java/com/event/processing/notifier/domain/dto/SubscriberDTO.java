package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
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
  private String customFields; // Json
  @JsonProperty("optin_ip")
  private String optinIp;
  @JsonProperty("optin_timestamp")
  private String optinTimestamp;
  @JsonProperty("created_at")
  private String createdAt;

  public SubscriberDTO(String id, String status, String email, String source,
                       String firstName, String lastName, String customFields,
                       String optinIp, Instant optinTimestamp, Instant createdAt) {
    this.id = id;
    this.status = status;
    this.email = email;
    this.source = source;
    this.firstName = firstName;
    this.lastName = lastName;
    this.customFields = customFields;
    this.optinIp = optinIp;
    this.optinTimestamp = String.valueOf(optinTimestamp);
    this.createdAt = String.valueOf(createdAt);
  }
}
