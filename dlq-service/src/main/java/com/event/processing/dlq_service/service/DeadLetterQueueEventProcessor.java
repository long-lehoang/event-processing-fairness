package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.domain.dto.DeadLetterQueueEventDTO;

import java.util.List;

/**
 * Interface defining the contract for dead letter queue event processing.
 * Following the Interface Segregation Principle, this interface provides
 * methods for handling dead letter queue events.
 */
public interface DeadLetterQueueEventProcessor {

  /**
   * Handles a batch of dead letter queue events.
   * This method processes multiple events in a single transaction for better performance.
   *
   * @param events The list of dead letter queue events to process
   */
  void handleDeadLetterEvents(List<DeadLetterQueueEventDTO> events);
}
