package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.service.DeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDeduplicationServiceImpl implements DeduplicationService {

  private final StringRedisTemplate redisTemplate;
  private static final String KEY_PREFIX = "deduplicate:event:";
  private static final Duration EXPIRY_TIME = Duration.ofMinutes(30);  // Configurable

  @Override
  public boolean isDuplicate(String eventId) {
    String key = KEY_PREFIX + eventId;
    Boolean exists = redisTemplate.hasKey(key);
    log.debug("Checking duplicate for eventId {}: {}", eventId, exists);
    return Boolean.TRUE.equals(exists);
  }

  @Override
  public void markProcessed(String eventId) {
    String key = KEY_PREFIX + eventId;
    redisTemplate.opsForValue().set(key, "1", EXPIRY_TIME);
    log.info("Marked event {} as processed in Redis", eventId);
  }
}
