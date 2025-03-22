package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class WebhookEventDTO {
  @JsonProperty("event_id")
  private String eventId;
  @JsonProperty("event_type")
  private String eventType;
  @JsonProperty("account_id")
  private String accountId;
}
