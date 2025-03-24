package com.event.processing.notifier.service;

/**
 * Interface defining the contract for rate limiting functionality.
 * This interface provides a standardized way to control the frequency
 * of event processing based on configured rate limits.
 *
 * Key features:
 * - Event-based rate limiting
 * - Configurable rate limits
 * - Event identification
 * - Rate limit enforcement
 *
 * @author LongLe
 * @version 1.0
 */
public interface RateLimiterService {
  /**
   * Checks if an event is allowed to be processed based on rate limiting rules.
   * This method determines whether the event has exceeded the configured rate
   * limit
   * and should be processed or rejected.
   *
   * @param eventId The unique identifier of the event to check
   * @return true if the event is allowed to be processed, false if it should be
   *         rate limited
   */
  boolean isAllow(String eventId);
}
