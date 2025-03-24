package com.event.processing.notifier.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Kafka topic settings.
 * This class defines and configures Kafka topics used in the application,
 * including the Dead Letter Queue (DLQ) for failed message processing.
 *
 * Key features:
 * - Configurable Dead Letter Queue topic
 * - Adjustable number of partitions
 * - Configurable replication factor
 * - Automatic topic creation on application startup
 *
 * @author LongLe
 * @version 1.0
 */
@Configuration
public class KafkaTopicConfig {

  /**
   * Name of the Dead Letter Queue topic.
   * Defaults to 'webhook-event-dead-letter-queue' if not specified.
   */
  @Value("${spring.kafka.topic.dead-letter-queue-topic.name:webhook-event-dead-letter-queue}")
  private String dlqTopic;

  /**
   * Number of partitions for the Dead Letter Queue topic.
   * Defaults to 3 if not specified.
   */
  @Value("${spring.kafka.topic.dead-letter-queue-topic.partitions:3}")
  private int dlqPartitions;

  /**
   * Replication factor for the Dead Letter Queue topic.
   * Defaults to 1 if not specified.
   */
  @Value("${spring.kafka.topic.dead-letter-queue-topic.replication-factor:1}")
  private short dlqReplicationFactor;

  /**
   * Creates a new Kafka topic for webhook events.
   * This topic is used as a Dead Letter Queue for failed message processing.
   *
   * @return NewTopic instance configured with the specified parameters
   */
  @Bean
  public NewTopic webhookEventsTopic() {
    return new NewTopic(dlqTopic, dlqPartitions, dlqReplicationFactor);
  }
}