package com.event.processing.webhook_server.request;

import lombok.Data;

import java.util.List;

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
    private List<Segments> segments;
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
