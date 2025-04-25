package com.event.processing.notifier.converter;

import com.event.processing.notifier.domain.dto.DeadLetterQueueEventDTO;
import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import org.springframework.stereotype.Component;

/**
 * Converter class responsible for transforming webhook events to dead letter queue events.
 * This class follows the Single Responsibility Principle by focusing solely on event conversion.
 */
@Component
public class WebhookToDLQEventConverter {
  
  /**
   * Converts a WebhookEventDTO to a DeadLetterQueueEventDTO.
   * This method extracts relevant information from the webhook event and creates a
   * corresponding dead letter queue event with failure information.
   *
   * @param webhookEvent  The webhook event to convert
   * @param failureReason The reason why the event processing failed
   * @return A DeadLetterQueueEventDTO containing the event information and failure details
   */
  public DeadLetterQueueEventDTO convert(WebhookEventDTO webhookEvent, String failureReason) {
    return DeadLetterQueueEventDTO.builder()
        .eventId(webhookEvent.getEventId())
        .accountId(webhookEvent.getAccountId())
        .eventType(webhookEvent.getEventType())
        .failureReason(failureReason)
        .lastErrorMessage(failureReason)
        .build();
  }
}
