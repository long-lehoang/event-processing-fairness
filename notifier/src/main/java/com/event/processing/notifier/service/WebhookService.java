package com.event.processing.notifier.service;

import com.event.processing.notifier.domain.dto.SubscriberEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;

import java.util.Map;
import java.util.Set;

public interface WebhookService {
  void processWithRetry(String eventId, String url, SubscriberEventDTO payload);

  Map<String, SubscriberEventDTO> getPayloads(Set<String> eventIds);

  Map<String, String> getWebhookUrls(Set<String> eventIds);
}
