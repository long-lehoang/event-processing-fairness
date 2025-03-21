package com.event.processing.notifier.consumer;

import com.event.processing.notifier.application.WebhookEventProcessingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventKafkaConsumer implements EventConsumer {

  private final WebhookEventProcessingService eventProcessingService;
  private final MeterRegistry meterRegistry;
  private final ThreadPoolTaskExecutor kafkaConsumerExecutor;

  @Value("${spring.kafka.topic.webhook-event:webhook-events}")
  private String topic;

  @KafkaListener(
      topics = "${spring.kafka.topic.webhook-event:webhook-events}",
      groupId = "webhook-group",
      containerFactory = "kafkaListenerContainerFactory"
  )
  @Override
  public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
    log.info("Received {} events", records.size());

    Timer batchProcessingTimer = meterRegistry.timer("kafka.batch.processing.time");
    batchProcessingTimer.record(() -> {
      records.forEach(event -> kafkaConsumerExecutor.submit(() -> processEvent(event)));
      acknowledgment.acknowledge(); // Commit offset after batch is processed
    });

    log.info("Acknowledged {} events", records.size());
  }

  private void processEvent(ConsumerRecord<String, String> event) {
    String eventId = event.key();
    String payload = event.value();

    Timer eventProcessingTimer = meterRegistry.timer("kafka.event.processing.time", "eventId", eventId);
    eventProcessingTimer.record(() -> eventProcessingService.process(eventId, payload));

    log.debug("Event {} processed", eventId);
  }
}
