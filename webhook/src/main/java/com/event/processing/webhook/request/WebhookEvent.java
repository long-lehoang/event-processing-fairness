package com.event.processing.webhook.request;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class WebhookEvent {
  private String eventName;
  private String eventTime;
  private Subscriber subscriber;
  private String webhookId;

  private static class Subscriber {
    private String id;
    private String status;
    private String email;
    private String source;
    private String firstName;
    private String lastName;
    private Set<Segments> segments;
    private Object customFields;
    private String optinIp;
    private String optinTimestamp;
    private String createdAt;
  }

  private static class Segments {
    private String id;
    private String name;
  }
}
