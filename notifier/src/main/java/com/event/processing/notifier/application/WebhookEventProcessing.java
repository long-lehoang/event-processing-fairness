package com.event.processing.notifier.application;

import com.event.processing.notifier.domain.dto.SubscriberEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;

public interface WebhookEventProcessing {
  void process(String eventId, WebhookEventDTO eventPayload, String url, SubscriberEventDTO webhookPayload);
}
