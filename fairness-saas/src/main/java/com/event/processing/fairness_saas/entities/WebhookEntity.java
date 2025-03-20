package com.event.processing.fairness_saas.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table
@Data
public class WebhookEntity {
    @Id
    private String id;

    @Column(name = "post_url")
    private String eventType;

    @Column(name = "created_by")
    private String createdBy;

}
