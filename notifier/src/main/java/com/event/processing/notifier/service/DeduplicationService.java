package com.event.processing.notifier.service;

public interface DeduplicationService {
  boolean isDuplicate(String eventId);

  void markProcessed(String eventId);
}
