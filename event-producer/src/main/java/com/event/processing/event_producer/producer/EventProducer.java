package com.event.processing.event_producer.producer;

import com.event.processing.event_producer.event.WebhookEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventProducer{

  @Value("${spring.kafka.webhook-event-topic:webhook-events}")
  private String topic;

  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;

  public void publishEvent(WebhookEvent event) {
    kafkaTemplate.send(topic, event.getEventId(), event);
  }
}