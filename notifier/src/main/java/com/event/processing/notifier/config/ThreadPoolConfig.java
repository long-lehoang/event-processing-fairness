package com.event.processing.notifier.config;

import com.event.processing.notifier.util.KafkaConsumerThreadPoolProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration class for thread pool settings.
 * This class configures the thread pool used for Kafka consumer message
 * processing,
 * providing configurable thread pool properties for optimal performance.
 *
 * Key features:
 * - Configurable core pool size
 * - Adjustable maximum pool size
 * - Configurable queue capacity
 * - Named thread prefix for better monitoring
 * - Automatic thread pool initialization
 *
 * @author LongLe
 * @version 1.0
 */
@Configuration
public class ThreadPoolConfig {

  /**
   * Creates and configures a ThreadPoolTaskExecutor for Kafka consumer message
   * processing.
   * The executor is configured with properties from
   * KafkaConsumerThreadPoolProperties
   * and uses a specific thread name prefix for better monitoring.
   *
   * @param properties Configuration properties for the thread pool
   * @return Configured ThreadPoolTaskExecutor instance
   */
  @Bean(name = "kafkaConsumerExecutor")
  public ThreadPoolTaskExecutor kafkaConsumerExecutor(KafkaConsumerThreadPoolProperties properties) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getCoreSize());
    executor.setMaxPoolSize(properties.getMaxSize());
    executor.setQueueCapacity(properties.getQueueCapacity());
    executor.setThreadNamePrefix("KafkaConsumer-");
    executor.initialize();
    return executor;
  }

}
