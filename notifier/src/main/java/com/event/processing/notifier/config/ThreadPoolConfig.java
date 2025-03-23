package com.event.processing.notifier.config;

import com.event.processing.notifier.util.KafkaConsumerThreadPoolProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolConfig {

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
