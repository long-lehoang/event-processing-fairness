package com.event.processing.fairness_saas.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Table
@Entity
public class SubscriberEntity {
    @Id
    private String id;

    private String status;
    private String email;
    private String source;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @ElementCollection
    @CollectionTable(name = "subscriber_segments")
    private List<Map<String, Object>> segments;

    @ElementCollection
    @CollectionTable(name = "subscriber_custom_fields")
    @MapKeyColumn(name = "field_key")
    @Column(name = "field_value")
    private Map<String, String> customFields;

    @Column(name = "optin_ip")
    private String optinIp;

    @Column(name = "optin_timestamp")
    private Instant optinTimestamp;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
