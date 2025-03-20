package com.event.processing.fairness_saas.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table
@Data
public class WebhookEventEntity {
    @Id
    private String id;

    @Column(name = "event_name")
    private String eventType;

    @Column(name = "event_time")
    private Instant eventTime;

    @Column(name = "subscriber_id")
    private String subscriberId;

    @Column(name = "webhook_id")
    private String webhookId;
    
    @Column(name = "created_by")
    private String createdBy;

}
