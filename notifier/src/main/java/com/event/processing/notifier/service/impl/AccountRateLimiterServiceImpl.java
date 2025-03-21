package com.event.processing.notifier.service.impl;

import com.event.processing.notifier.config.RateLimitConfig;
import com.event.processing.notifier.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountRateLimiterServiceImpl implements RateLimiterService {


  private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:";

  private final StringRedisTemplate redisTemplate;
  private final RateLimitConfig rateLimitConfig;

  /**
   * Checks if a given account is allowed based on rate limits.
   *
   * @param accountId The account ID.
   * @return True if allowed, False if rate limit exceeded.
   */
  @Override
  public boolean isExceedLimit(String accountId) {
    String key = RATE_LIMIT_KEY_PREFIX + accountId;
    ValueOperations<String, String> ops = redisTemplate.opsForValue();

    try {
      long currentCount = Optional.of(ops.increment(key, 1)).orElse(1L);

      if (currentCount == 1) {
        redisTemplate.expire(key, Duration.ofMinutes(rateLimitConfig.getDurationMinutes()));
      }

      boolean allowed = currentCount <= rateLimitConfig.getLimit();
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