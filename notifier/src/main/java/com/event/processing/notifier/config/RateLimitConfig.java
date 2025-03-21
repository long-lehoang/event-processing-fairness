package com.event.processing.notifier.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class RateLimitConfig {
  @Value("${spring.redis.limit.event:100}")
  private int limit;
  @Value("${spring.redis.limit.time:1}")
  private int durationMinutes;
}
