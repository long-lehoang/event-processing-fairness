package com.event.processing.dlq_service.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WebhookEventDTO {
  @JsonProperty("event_id")
  private String eventId;

  @JsonProperty("event_type")
  private String eventType;

  @JsonProperty("account_id")
  private String accountId;
}