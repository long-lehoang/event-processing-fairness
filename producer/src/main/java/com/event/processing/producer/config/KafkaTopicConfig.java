package com.event.processing.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic webhookEventsTopic() {
    return new NewTopic("webhook-events", 3, (short) 1); // 3 partitions, 1 replication
  }
}