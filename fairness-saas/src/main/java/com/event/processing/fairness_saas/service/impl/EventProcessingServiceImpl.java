package com.event.processing.fairness_saas.service.impl;

import org.springframework.stereotype.Service;

import com.event.processing.fairness_saas.event.WebHookEvent;
import com.event.processing.fairness_saas.service.EventProcessingService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class EventProcessingServiceImpl implements EventProcessingService {

    private RateLimiterRepository rateLimiterRepository;
    private WebhookCaller webhookCaller;
    private static final String CIRCUIT_BREAKER_NAME = "webhookCircuitBreaker";

    @Override
    public void processEvent(WebHookEvent event) {
        if (rateLimiterRepository.isRateLimited(event.getAccountId())) {
            throw new RuntimeException("Rate limit exceeded, pushing event back");
        }

        sendWebhook(event);

    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackWebhook")
    public void sendWebhook(Event event) {
        webhookCaller.callWebhook(event);
    }

    public void fallbackWebhook(Event event, Throwable t) {
        System.out.println("Circuit Breaker Open. Moving to Dead Letter Queue: " + event);
        // Logic to push to Dead Letter Queue
    }
}
