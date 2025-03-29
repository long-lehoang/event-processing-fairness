package com.event.processing.notifier.client;

import com.event.processing.notifier.domain.dto.BaseEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * REST-based implementation of WebhookClient for delivering webhook
 * notifications.
 * This class provides HTTP-based webhook delivery functionality using Spring's
 * RestClient,
 * with comprehensive logging and error handling.
 * <p>
 * Key features:
 * - HTTP POST-based webhook delivery
 * - Response status code validation
 * - Detailed logging of requests and responses
 * - Exception handling and propagation
 *
 * @author LongLe
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookRestClient implements WebhookClient {

  /**
   * Spring RestClient instance for making HTTP requests.
   * Configured with appropriate settings for webhook delivery.
   */
  private final RestClient restClient;

  /**
   * Sends a webhook notification using HTTP POST.
   * This method:
   * - Logs the webhook request details
   * - Sends the payload to the specified URL
   * - Validates the response status code
   * - Logs the response or any errors
   *
   * @param webhookUrl The destination URL for the webhook notification
   * @param payload    The event payload to be sent in the webhook
   * @return true if the webhook was successfully delivered (2xx status code),
   * false otherwise
   * @throws Exception if the webhook delivery fails
   */
  @Override
  public boolean sendWebhook(String webhookUrl, BaseEventDTO payload) {
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
