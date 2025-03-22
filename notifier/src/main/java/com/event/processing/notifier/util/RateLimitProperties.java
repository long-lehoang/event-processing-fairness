package com.event.processing.notifier.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.redis.limit")
public class RateLimitProperties {
  private int event = 100; // Default value
  private int time = 1;    // Default duration in minutes
}