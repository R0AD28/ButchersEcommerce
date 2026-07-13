package com.ecommerce.commerce_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditOutbox {
    @Id
    private UUID id;
    private Long userId;
    private String userEmail;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String resourceType;
    private String resourceId;
    @Column(nullable = false)
    private String result;
    private String ipAddress;
    @Column(length = 2000)
    private String details;
    private String correlationId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    @Column(nullable = false)
    private int attempts;
    @Column(nullable = false)
    private Instant nextAttemptAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant sentAt;

    public enum Status {
        PENDING, SENT
    }
}
