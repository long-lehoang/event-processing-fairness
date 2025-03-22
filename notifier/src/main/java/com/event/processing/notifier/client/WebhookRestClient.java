package com.event.processing.notifier.client;

import com.event.processing.notifier.domain.dto.SubscriberEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookRestClient implements WebhookClient {

  private final RestClient restClient;

  @Override
  public boolean sendWebhook(String webhookUrl, SubscriberEventDTO payload) {
    log.info("Sending webhook event: url {} payload {}", webhookUrl, payload);

    try {
      ResponseEntity<String> response = restClient.post()
          .uri(webhookUrl)
          .body(payload)
          .retrieve()
          .toEntity(String.class);

      log.info("Webhook success: {}, Response: {}", webhookUrl, response.getBody());
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception ex) {
      log.error("Webhook failed: {}, Error: {}", webhookUrl, ex.getMessage(), ex);
      throw ex;
    }
  }
}
