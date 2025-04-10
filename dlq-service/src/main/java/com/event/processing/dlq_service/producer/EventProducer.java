package com.event.processing.dlq_service.producer;

import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Interface defining the contract for event producers.
 * Following the Interface Segregation Principle, this interface provides
 * a generic method for publishing events to a message broker.
 */
public interface EventProducer<T> {

  /**
   * Publishes an event to the configured topic.
   *
   * @param event The event to publish
   * @return A CompletableFuture that will be completed when the send operation completes
   */
  CompletableFuture<SendResult<String, T>> publishEvent(T event);

  /**
   * Publishes an event to a specific topic.
   *
   * @param topic The topic to publish to
   * @param event The event to publish
   * @return A CompletableFuture that will be completed when the send operation completes
   */
  CompletableFuture<SendResult<String, T>> publishEvent(String topic, T event);
}
