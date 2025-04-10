package com.event.processing.dlq_service.domain.mapper;

import com.event.processing.dlq_service.domain.dto.DeadLetterQueueEventDTO;
import com.event.processing.dlq_service.domain.dto.WebhookEventDTO;
import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeadLetterQueueMapper {
  private final ObjectMapper objectMapper;

  public DeadLetterQueueEventDTO toDto(DeadLetterEvent entity) {
    return DeadLetterQueueEventDTO.builder()
        .eventId(entity.getEventId())
        .accountId(entity.getAccountId())
        .eventType(entity.getEventType())
        .lastErrorMessage(entity.getLastErrorMessage())
        .failureReason(entity.getFailureReason())
        .build();
  }

  public DeadLetterEvent toEntity(DeadLetterQueueEventDTO dto) {
    return DeadLetterEvent.builder()
        .eventId(dto.getEventId())
        .accountId(dto.getAccountId())
        .eventType(dto.getEventType())
        .lastErrorMessage(dto.getLastErrorMessage())
        .failureReason(dto.getFailureReason())
        .build();
  }

  public WebhookEventDTO toWebhookEventDTO(DeadLetterEvent entity) {
    return WebhookEventDTO.builder()
        .eventId(entity.getEventId())
        .accountId(entity.getAccountId())
        .eventType(entity.getEventType())
        .build();
  }
}