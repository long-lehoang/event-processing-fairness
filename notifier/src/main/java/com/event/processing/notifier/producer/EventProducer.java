package com.event.processing.notifier.producer;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;

public interface EventProducer {
  void publish(String topic, String id, WebhookEventDTO payload);
}
