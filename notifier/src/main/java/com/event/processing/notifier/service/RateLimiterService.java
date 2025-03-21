package com.event.processing.notifier.service;

public interface RateLimiterService {
  boolean isExceedLimit(String eventId);
}
