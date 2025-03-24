package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.service.DeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-based implementation of DeduplicationService for event deduplication.
 * This class provides event deduplication functionality using Redis as a
 * distributed
 * cache, ensuring consistent deduplication across multiple instances of the
 * application.
 *
 * Key features:
 * - Redis-based distributed deduplication
 * - Configurable event expiry time
 * - Automatic cleanup of old events
 * - Detailed logging of deduplication checks
 *
 * @author LongLe
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDeduplicationServiceImpl implements DeduplicationService {

  /**
   * Prefix used for Redis keys to identify deduplication entries.
   */
  private static final String KEY_PREFIX = "deduplicate:event:";

  /**
   * Duration after which deduplication entries expire from Redis.
   * This ensures automatic cleanup of old event records.
   */
  private static final Duration EXPIRY_TIME = Duration.ofMinutes(30); // Configurable

  private final StringRedisTemplate redisTemplate;

  /**
   * Checks if an event has already been processed.
   * This method uses Redis to check for the existence of an event record:
   * - Constructs a unique key for the event
   * - Checks if the key exists in Redis
   * - Logs the result of the check
   *
   * @param eventId The unique identifier of the event to check
   * @return true if the event is a duplicate and should be skipped, false
   *         otherwise
   */
  @Override
  public boolean isDuplicate(String eventId) {
    String key = KEY_PREFIX + eventId;
    Boolean exists = redisTemplate.hasKey(key);
    log.debug("Checking duplicate for eventId {}: {}", eventId, exists);
    return Boolean.TRUE.equals(exists);
  }

  /**
   * Marks an event as processed to prevent future duplicate processing.
   * This method stores the event record in Redis with an expiration time:
   * - Constructs a unique key for the event
   * - Stores the event record with expiration
   * - Logs the successful marking of the event
   *
   * @param eventId The unique identifier of the processed event
   */
  @Override
  public void markProcessed(String eventId) {
    String key = KEY_PREFIX + eventId;
    redisTemplate.opsForValue().set(key, "1", EXPIRY_TIME);
    log.info("Marked event {} as processed in Redis", eventId);
  }
}
