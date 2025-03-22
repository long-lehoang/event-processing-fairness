package com.event.processing.producer.controller;

import com.event.processing.producer.event.WebhookEventDTO;
import com.event.processing.producer.producer.EventProducer;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook-events")
@AllArgsConstructor
public class EventController {
  private EventProducer eventProducer;

  @PostMapping("/publish")
  public String publishEvent(@RequestBody WebhookEventDTO event) {
    eventProducer.publishEvent(event);
    return "Event published successfully";
  }
}
