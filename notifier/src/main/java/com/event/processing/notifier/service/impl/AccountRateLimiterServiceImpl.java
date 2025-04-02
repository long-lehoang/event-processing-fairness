package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.service.RateLimiterService;
import com.event.processing.notifier.util.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of RateLimiterService for account-level rate
 * limiting.
 * This class provides rate limiting functionality using Redis as a distributed
 * counter,
 * ensuring consistent rate limiting across multiple instances of the
 * application.
 * <p>
 * Key features:
 * - Redis-based distributed rate limiting
 * - Configurable rate limits and time windows
 * - Fail-safe behavior on Redis failures
 * - Detailed logging of rate limit violations
 *
 * @author LongLe
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountRateLimiterServiceImpl implements RateLimiterService {

  /**
   * Prefix used for Redis keys to identify rate limit counters.
   */
  private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:";
  
  /**
   * Lua script for atomically checking and incrementing rate limit counters.
   * Optimizes the batch operation by using a single Redis call.
   */
  private static final String CHECK_AND_INCREMENT_SCRIPT = 
      "local key = KEYS[1] " +
      "local count = tonumber(ARGV[1]) " +
      "local limit = tonumber(ARGV[2]) " +
      "local ttl = tonumber(ARGV[3]) " +
      "local exists = redis.call('EXISTS', key) " +
      "local current = 0 " +
      "if exists == 0 then " +
      "  redis.call('SET', key, count, 'EX', ttl) " +
      "  return 1 " + 
      "else " +
      "  current = tonumber(redis.call('GET', key)) " +
      "  if (current + count) <= limit then " +
      "    redis.call('INCRBY', key, count) " +
      "    local keyTTL = redis.call('TTL', key) " +
      "    if keyTTL == -1 then " +
      "      redis.call('EXPIRE', key, ttl) " +
      "    end " +
      "    return 1 " +
      "  else " +
      "    return 0 " +
      "  end " +
      "end";

  private final StringRedisTemplate redisTemplate;
  private final RateLimitProperties rateLimitProperties;

  /**
   * Checks if an account is allowed to process events based on rate limits.
   * This method implements a sliding window rate limiter using Redis:
   * - Increments a counter for the account
   * - Sets expiration on first increment
   * - Checks against configured rate limit
   * - Implements fail-safe behavior on Redis failures
   *
   * @param accountId The unique identifier of the account to check
   * @return true if the account is within rate limits, false if rate limit
   * exceeded
   */
  @Override
  public boolean isAllow(String accountId) {
    return areEventsAllowed(accountId, 1);
  }
  
  /**
   * Checks if multiple events for an account can be processed without exceeding rate limits.
   * This method efficiently uses a Lua script to perform the check and increment in a single
   * Redis operation, reducing network overhead for batch operations.
   *
   * @param accountId The account ID to check
   * @param count The number of events to check
   * @return true if the events can be processed without exceeding rate limits, false otherwise
   */
  @Override
  public boolean areEventsAllowed(String accountId, int count) {
    String key = RATE_LIMIT_KEY_PREFIX + accountId;
    long expiryTime = TimeUnit.MINUTES.toSeconds(rateLimitProperties.getTime());
    long eventLimit = rateLimitProperties.getEvent();
    
    try {
      // Use Lua script to atomically check and increment counter
      Boolean allowed = redisTemplate.execute(
          RedisScript.of(CHECK_AND_INCREMENT_SCRIPT, Boolean.class),
          Collections.singletonList(key),
          String.valueOf(count),
          String.valueOf(eventLimit),
          String.valueOf(expiryTime)
      );
      
      if (Boolean.FALSE.equals(allowed)) {
        log.warn("Rate limit exceeded for account: {}. Attempted to process {} events", accountId, count);
        return false;
      }
      
      return true;
    } catch (Exception e) {
      log.error("Error checking rate limit for account: {} with count {}", accountId, count, e);
      return true; // Fail-safe: Allow request in case of Redis failure
    }
  }
}