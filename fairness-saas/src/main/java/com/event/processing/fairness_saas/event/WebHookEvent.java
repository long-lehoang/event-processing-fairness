package com.event.processing.fairness_saas.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebHookEvent {
    private String eventType;
    private String eventId;
    private String accountId;
}
