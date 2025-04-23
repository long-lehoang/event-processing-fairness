package com.event.processing.dlq_service.controller;

import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import com.event.processing.dlq_service.repository.DeadLetterEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
public class DeadLetterQueueController {
  private final DeadLetterEventRepository repository;
  private final MeterRegistry meterRegistry;

  @GetMapping("/events")
  public Page<DeadLetterEvent> getEvents(Pageable pageable) {
    return repository.findAll(pageable);
  }

  @GetMapping("/events/{eventId}")
  public DeadLetterEvent getEvent(@PathVariable String eventId) {
    return repository.findById(eventId)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
  }
}