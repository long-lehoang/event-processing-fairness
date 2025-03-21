package com.event.processing.notifier.service;

public interface WebhookService {
  void processWithRetry(String eventId, String payload);
}
