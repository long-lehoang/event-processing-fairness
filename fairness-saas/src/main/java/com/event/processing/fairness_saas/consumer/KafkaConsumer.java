package com.event.processing.fairness_saas.consumer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.event.processing.fairness_saas.event.WebHookEvent;
import com.event.processing.fairness_saas.service.EventProcessingService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class KafkaConsumer {
    private EventProcessingService eventProcessingService;

    @KafkaListener(topics = "webhook-events", groupId = "event-consumers", containerFactory = "kafkaListenerContainerFactory")
    public void consumeEvents(List<WebHookEvent> events, Acknowledgment ack) {
        try {
            for (WebHookEvent event : events) {
                eventProcessingService.processEvent(event);
            }
            ack.acknowledge(); // Manual commit after processing batch
        } catch (Exception e) {
            // Handle error, don't commit if failure
            e.printStackTrace();
        }
    }
}
