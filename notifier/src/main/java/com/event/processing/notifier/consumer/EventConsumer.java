package com.event.processing.notifier.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

public interface EventConsumer {
  void consume(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment);
}
