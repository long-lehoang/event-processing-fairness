package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.service.RateLimiterService;
import com.event.processing.notifier.util.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

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
    String key = RATE_LIMIT_KEY_PREFIX + accountId;
    ValueOperations<String, String> ops = redisTemplate.opsForValue();

    try {
      long currentCount = Optional.of(ops.increment(key, 1)).orElse(1L);

      if (currentCount == 1) {
        redisTemplate.expire(key, Duration.ofMinutes(rateLimitProperties.getTime()));
      }

      boolean allowed = currentCount <= rateLimitProperties.getEvent();
      if (!allowed) {
        log.warn("Rate limit exceeded for account: {}", accountId);
      }
      return allowed;

    } catch (Exception e) {
      log.error("Error checking rate limit for account: {}", accountId, e);
      return true; // Fail-safe: Allow request in case of Redis failure
    }
  }
}