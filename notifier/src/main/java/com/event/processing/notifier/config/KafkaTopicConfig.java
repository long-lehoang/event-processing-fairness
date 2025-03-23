package com.event.processing.notifier.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

  @Value("${spring.kafka.topic.dead-letter-queue-topic.name:webhook-event-dead-letter-queue}")
  private String dlqTopic;

  @Value("${spring.kafka.topic.dead-letter-queue-topic.partitions:3}")
  private int dlqPartitions;

  @Value("${spring.kafka.topic.dead-letter-queue-topic.replication-factor:1}")
  private short dlqReplicationFactor;

  @Bean
  public NewTopic webhookEventsTopic() {
    return new NewTopic(dlqTopic, dlqPartitions, dlqReplicationFactor);
  }
}