package com.event.processing.notifier.client;

import com.event.processing.notifier.domain.dto.SubscriberEventDTO;

public interface WebhookClient {
  boolean sendWebhook(String url, SubscriberEventDTO payload);
}