package com.event.processing.notifier.monitoring;


import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaLagMetricsConfig {

  @Bean
  public KafkaClientMetrics kafkaMetrics(KafkaConsumer<String, String> consumer) {
    return new KafkaClientMetrics(consumer);
  }
}

