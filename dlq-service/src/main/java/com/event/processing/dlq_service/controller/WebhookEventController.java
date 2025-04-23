package com.event.processing.dlq_service.controller;

import com.event.processing.dlq_service.constants.EventStatusConstants;
import com.event.processing.dlq_service.service.RetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.concurrent.Executor;

/**
 * Controller for webhook event operations.
 * Provides endpoints for publishing webhook events.
 * Following the Single Responsibility Principle, this controller only handles
 * HTTP requests related to webhook events.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook-events")
@RequiredArgsConstructor
public class WebhookEventController {

  private final RetryService retryService;

  /**
   * Retries failed events from the dead letter queue.
   *
   * @param status The status of events to retry (optional, defaults to "PENDING")
   * @return A list of event IDs that were retried
   */
  @PostMapping("/retry")
  public ResponseEntity<String> retryEvents(
      @RequestParam(required = false, defaultValue = EventStatusConstants.PENDING) String status) {
    log.info("Received request to retry events with status: {}", status);

    retryService.processRetries(status, Instant.now());
    return ResponseEntity.ok("Events retried successfully");
  }
}
