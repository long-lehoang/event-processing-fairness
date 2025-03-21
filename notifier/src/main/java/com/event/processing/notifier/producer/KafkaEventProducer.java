package com.event.processing.notifier.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducer implements EventProducer{
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Override
  public void publish(String topic, String key, String payload) {
    kafkaTemplate.send(topic, key, payload);
    //TODO: Update callback to log failed or success
  }
}
