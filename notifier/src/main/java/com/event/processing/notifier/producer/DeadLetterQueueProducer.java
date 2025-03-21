package com.event.processing.notifier.producer;

public interface DeadLetterQueueProducer {
  void publish(String topic, String id, String payload);
}
