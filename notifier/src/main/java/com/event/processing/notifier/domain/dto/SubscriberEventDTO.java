package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class SubscriberEventDTO {
  @JsonIgnore
  private String id;
  @JsonProperty("event_name")
  private String eventName;
  @JsonProperty("event_time")
  private String eventTime;
  @JsonProperty("subscriber")
  private SubscriberDTO subscriber;
  @JsonProperty("webhook_id")
  private String webhookId;

  public SubscriberEventDTO(String id, String eventName, Instant eventTime, SubscriberDTO subscriber, String webhookId) {
    this.id = id;
    this.eventName = eventName;
    this.eventTime = String.valueOf(eventTime);
    this.subscriber = subscriber;
    this.webhookId = webhookId;
  }
}
