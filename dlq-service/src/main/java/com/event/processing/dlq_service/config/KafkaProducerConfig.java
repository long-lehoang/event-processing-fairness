package com.event.processing.dlq_service.config;

import com.event.processing.dlq_service.domain.dto.WebhookEventDTO;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Kafka producer settings.
 * This class configures the Kafka producer properties and creates necessary
 * beans for producing messages to Kafka topics.
 */
@Configuration
public class KafkaProducerConfig {

  /**
   * Kafka bootstrap servers configuration.
   * Retrieved from application properties.
   */
  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  /**
   * Creates a producer factory for WebhookEventDTO messages.
   *
   * @return Configured ProducerFactory instance for WebhookEventDTO
   */
  @Bean
  public ProducerFactory<String, WebhookEventDTO> webhookProducerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, KafkaPartitioner.class);
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * Creates a KafkaTemplate for sending WebhookEventDTO messages to Kafka topics.
   * The template is thread-safe and can be used across multiple threads.
   *
   * @return Configured KafkaTemplate instance for WebhookEventDTO
   */
  @Bean("webhookKafkaTemplate")
  public KafkaTemplate<String, WebhookEventDTO> webhookKafkaTemplate() {
    return new KafkaTemplate<>(webhookProducerFactory());
  }
}