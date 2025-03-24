package com.event.processing.notifier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration class for REST client settings.
 * This class provides configuration for the RestClient used in the application
 * for making HTTP requests to external services.
 *
 * Key features:
 * - Singleton RestClient instance
 * - Thread-safe configuration
 * - Spring-managed bean lifecycle
 *
 * @author LongLe
 * @version 1.0
 */
@Configuration
public class RestClientConfig {

  /**
   * Creates and configures a RestClient instance for making HTTP requests.
   * The RestClient is configured as a singleton bean and is thread-safe.
   *
   * @return Configured RestClient instance
   */
  @Bean
  public RestClient restClient() {
    return RestClient.create();
  }
}