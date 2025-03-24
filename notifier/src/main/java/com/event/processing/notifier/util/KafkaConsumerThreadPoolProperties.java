package com.event.processing.notifier.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Kafka consumer thread pool.
 * This class defines the thread pool parameters used for parallel processing
 * of Kafka consumer records.
 *
 * Key features:
 * - Configurable core pool size
 * - Configurable maximum pool size
 * - Configurable queue capacity
 * - Spring Boot configuration properties support
 *
 * @author LongLe
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "thread-pool.kafka-consumer")
@Getter
@Setter
public class KafkaConsumerThreadPoolProperties {
  /**
   * The core number of threads to keep alive even when idle.
   * This is the minimum number of threads that will be maintained in the pool.
   */
  private int coreSize;

  /**
   * The maximum number of threads to allow in the pool.
   * When the queue is full, new threads will be created up to this maximum.
   */
  private int maxSize;

  /**
   * The capacity of the queue used for holding tasks before they are executed.
   * When the queue is full, new threads will be created up to maxSize.
   */
  private int queueCapacity;
}
