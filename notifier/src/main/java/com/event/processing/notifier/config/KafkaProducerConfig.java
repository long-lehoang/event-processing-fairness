package com.event.processing.notifier.config;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;
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
 * beans
 * for producing messages to Kafka topics.
 *
 * Key features:
 * - JSON serialization for WebhookEventDTO
 * - String key serialization
 * - Configurable bootstrap servers
 * - Thread-safe KafkaTemplate for message sending
 *
 * @author LongLe
 * @version 1.0
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
   * Creates and configures the Kafka ProducerFactory.
   * Sets up serializers and producer properties for sending WebhookEventDTO
   * messages.
   *
   * @return Configured ProducerFactory instance
   */
  @Bean
  public ProducerFactory<String, WebhookEventDTO> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * Creates a KafkaTemplate for sending messages to Kafka topics.
   * The template is thread-safe and can be used across multiple threads.
   *
   * @return Configured KafkaTemplate instance
   */
  @Bean
  public KafkaTemplate<String, WebhookEventDTO> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}
