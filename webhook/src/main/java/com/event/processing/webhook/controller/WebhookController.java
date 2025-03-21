package com.event.processing.webhook.controller;

import com.event.processing.webhook.request.WebhookEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {
  @PostMapping("/success")
  public ResponseEntity<String> testSuccess(WebhookEvent event){
    return ResponseEntity.ok("OK");
  }

  @PostMapping("/failed")
  public ResponseEntity<String> testFailed(WebhookEvent event){
    return ResponseEntity.badRequest().build();
  }
}
