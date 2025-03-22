package com.event.processing.producer.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

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