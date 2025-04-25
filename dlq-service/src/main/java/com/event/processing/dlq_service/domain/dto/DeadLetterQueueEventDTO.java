package com.event.processing.dlq_service.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterQueueEventDTO {
  @JsonProperty("event_id")
  private String eventId;

  @JsonProperty("account_id")
  private String accountId;

  @JsonProperty("event_type")
  private String eventType;

  @JsonProperty("last_error_message")
  private String lastErrorMessage;

  @JsonProperty("failure_reason")
  private String failureReason;
}