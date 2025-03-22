package com.event.processing.notifier.producer;

import com.event.processing.notifier.domain.dto.WebhookEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeadLetterQueueProducer implements DeadLetterQueueProducer {

  private final KafkaTemplate<String, WebhookEventDTO> kafkaTemplate;

  @Override
  public void publish(String topic, String id, WebhookEventDTO payload) {
    log.info("Publishing event to DLQ: Topic={}, Key={}, Payload={}", topic, id, payload);

    CompletableFuture<SendResult<String, WebhookEventDTO>> future = kafkaTemplate.send(topic, id, payload);

    future.thenAccept(result -> {
      RecordMetadata metadata = result.getRecordMetadata();
      log.info("DLQ Event Sent Successfully: Topic={}, Partition={}, Offset={}",
          metadata.topic(), metadata.partition(), metadata.offset());
    }).exceptionally(ex -> {
      log.error("Failed to publish event to DLQ: Topic={}, Key={}, Error={}",
          topic, id, ex.getMessage(), ex);
      return null;
    });
  }
}
