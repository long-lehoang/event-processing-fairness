package com.event.processing.dlq_service.config;

import com.event.processing.dlq_service.domain.dto.DeadLetterQueueEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

  /**
   * Kafka bootstrap servers configuration.
   * Retrieved from application properties.
   */
  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  /**
   * Consumer group ID for Kafka consumer group management.
   * Defaults to 'event-processing-group' if not specified.
   */
  @Value("${spring.kafka.consumer.group-id:event-processing-group}")
  private String groupId;

  /**
   * Flag to enable/disable auto commit of offsets.
   * Defaults to false for manual commit control.
   */
  @Value("${spring.kafka.consumer.enable-auto-commit:false}")
  private boolean enableAutoCommit;

  /**
   * Maximum time to wait for data when polling Kafka.
   * Defaults to 3000ms if not specified.
   */
  @Value("${spring.kafka.consumer.poll-timeout:3000}")
  private int pollTimeout;

  /**
   * Maximum number of records to fetch in a single poll.
   * Defaults to 20 records if not specified.
   */
  @Value("${spring.kafka.consumer.max-poll-records:20}")
  private int maxPollRecords;

  /**
   * Number of concurrent threads for message processing.
   * Defaults to 1 if not specified.
   */
  @Value("${spring.kafka.listener.concurrency:1}")
  private int concurrency;

  /**
   * Creates a consumer factory for DeadLetterQueueEventDTO messages.
   * Configures the deserializers and consumer properties.
   *
   * @return Configured ConsumerFactory instance
   */
  @Bean
  public ConsumerFactory<String, DeadLetterQueueEventDTO> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
    props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, "org.apache.kafka.clients.consumer.RoundRobinAssignor");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000); // 3 seconds
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, true);
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DeadLetterQueueEventDTO.class.getName());

    log.info("Creating Kafka Consumer Factory with Group ID: {}, Max Poll Records: {}, Enable Auto Commit: {}",
        groupId, maxPollRecords, enableAutoCommit);

    // Create JsonDeserializer with proper type information
    JsonDeserializer<DeadLetterQueueEventDTO> jsonDeserializer = new JsonDeserializer<>(DeadLetterQueueEventDTO.class);
    jsonDeserializer.setRemoveTypeHeaders(false);
    jsonDeserializer.addTrustedPackages("*");
    jsonDeserializer.setUseTypeMapperForKey(true);

    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
  }

  /**
   * Creates and configures the Kafka Listener Container Factory.
   * Sets up batch processing, concurrency, and acknowledgment mode.
   *
   * @return Configured ConcurrentKafkaListenerContainerFactory instance
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, DeadLetterQueueEventDTO> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, DeadLetterQueueEventDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setBatchListener(true); // Enable batch processing
    factory.setConcurrency(concurrency);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    factory.getContainerProperties().setPollTimeout(pollTimeout);

    log.info(
        "Initializing Kafka Batch Listener Factory with Concurrency: {}, Poll Timeout: {}, Ack Mode: MANUAL_IMMEDIATE",
        concurrency, pollTimeout);

    return factory;
  }
}