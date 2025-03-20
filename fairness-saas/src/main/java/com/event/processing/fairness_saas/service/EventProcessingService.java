package com.event.processing.fairness_saas.service;

import com.event.processing.fairness_saas.event.WebHookEvent;

public interface EventProcessingService {

    void processEvent(WebHookEvent event);

}
