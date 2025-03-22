package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class SubscriberEventDTO {
  @JsonProperty("event_name")
  private String eventName;
  @JsonProperty("event_time")
  private String eventTime;
  @JsonProperty("subscriber")
  private SubscriberDTO subscriber;
  @JsonProperty("webhook_id")
  private String webhookId;
}
