package com.event.processing.notifier.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for rate limiting functionality.
 * This class defines the rate limiting parameters used to control the frequency
 * of webhook event processing.
 * <p>
 * Key features:
 * - Configurable event rate limit
 * - Configurable time window
 * - Default values for both parameters
 *
 * @author LongLe
 * @version 1.0
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis.limit")
public class RateLimitProperties {
  /**
   * Maximum number of events allowed within the time window.
   * Default value is 100 events.
   */
  private int event = 100; // Default value

  /**
   * Time window duration in minutes for rate limiting.
   * Default value is 1 minute.
   */
  private int time = 1; // Default duration in minutes
}