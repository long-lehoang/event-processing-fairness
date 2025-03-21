package com.event.processing.notifier.application;

public interface WebhookEventProcessingService {
  void process(String eventId, String payload);
}
