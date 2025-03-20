package com.event.processing.event_producer.controller;

import com.event.processing.event_producer.event.WebhookEvent;
import com.event.processing.event_producer.producer.EventProducer;
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
  public String publishEvent(@RequestBody WebhookEvent event) {
    eventProducer.publishEvent(event);
    return "Event published successfully";
  }
}
