package com.event.processing.dlq_service.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dead_letter_events")
public class DeadLetterEvent {
    @Id
    private String eventId;
    
    @Column(nullable = false)
    private String accountId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private Integer retryCount;
    
    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column
    private Instant lastRetryAt;
    
    @Column
    private String lastErrorMessage;
    
    @Column
    private String failureReason;
}