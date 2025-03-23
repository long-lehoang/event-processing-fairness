package com.event.processing.notifier.config;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;
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

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id:event-processing-group}")
  private String groupId;

  @Value("${spring.kafka.consumer.enable-auto-commit:false}")
  private boolean enableAutoCommit;

  @Value("${spring.kafka.consumer.poll-timeout:3000}")
  private int pollTimeout;

  @Value("${spring.kafka.consumer.max-poll-records:20}")
  private int maxPollRecords;

  @Value("${spring.kafka.listener.concurrency:1}")
  private int concurrency;

  @Bean
  public ConsumerFactory<String, WebhookEventDTO> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, true);

    log.info("Creating Kafka Consumer Factory with Group ID: {}, Max Poll Records: {}, Enable Auto Commit: {}",
        groupId, maxPollRecords, enableAutoCommit);

    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
        new JsonDeserializer<>(WebhookEventDTO.class, false));
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, WebhookEventDTO> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, WebhookEventDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setBatchListener(true); // Enable batch processing
    factory.setConcurrency(concurrency);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    factory.getContainerProperties().setPollTimeout(pollTimeout);

    log.info("Initializing Kafka Batch Listener Factory with Concurrency: {}, Poll Timeout: {}, Ack Mode: MANUAL_IMMEDIATE",
        concurrency, pollTimeout);

    return factory;
  }
}
