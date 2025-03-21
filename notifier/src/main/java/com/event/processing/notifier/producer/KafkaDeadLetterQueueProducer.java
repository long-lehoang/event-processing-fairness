package com.event.processing.notifier.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaDeadLetterQueueProducer implements DeadLetterQueueProducer{
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Override
  public void publish(String topic, String id, String payload) {
    kafkaTemplate.send(topic, id, payload);
    //TODO: Update callback to log failed or success
  }
}
