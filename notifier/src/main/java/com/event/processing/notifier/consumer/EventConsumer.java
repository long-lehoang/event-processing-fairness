package com.event.processing.notifier.consumer;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

public interface EventConsumer {
  void consume(List<ConsumerRecord<String, WebhookEventDTO>> records, Acknowledgment acknowledgment);
}
