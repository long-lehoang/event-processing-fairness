package com.event.processing.producer.producer;

import com.event.processing.producer.event.WebhookEventDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventProducer{

  @Value("${spring.kafka.webhook-event-topic:webhook-events}")
  private String topic;

  @Autowired
  private KafkaTemplate<String, WebhookEventDTO> kafkaTemplate;

  public void publishEvent(WebhookEventDTO event) {
    kafkaTemplate.send(topic, event.getEventId(), event);
  }
}