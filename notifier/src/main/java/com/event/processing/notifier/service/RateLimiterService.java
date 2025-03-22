package com.event.processing.notifier.service;

public interface RateLimiterService {
  boolean isAllow(String eventId);
}
