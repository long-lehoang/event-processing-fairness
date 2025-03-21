package com.event.processing.notifier.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "thread-pool.kafka-consumer")
@Getter
@Setter
public class KafkaConsumerThreadPoolProperties {
  private int coreSize;
  private int maxSize;
  private int queueCapacity;
}

