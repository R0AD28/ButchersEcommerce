package com.ecommerce.audit_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de auditoría almacenado de manera append-only.
 *
 * La API no expone operaciones de actualización o eliminación. Los hashes
 * permiten detectar alteraciones realizadas directamente sobre la base.
 */
@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_audit_recorded_at", columnList = "recorded_at"),
                @Index(name = "idx_audit_user_email", columnList = "user_email"),
                @Index(name = "idx_audit_service_action", columnList = "source_service, action"),
                @Index(name = "idx_audit_correlation_id", columnList = "correlation_id"),
                @Index(name = "idx_audit_critical", columnList = "critical")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "user_email", length = 254)
    private String userEmail;

    @Column(name = "source_service", nullable = false, length = 80)
    private String sourceService;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(length = 120)
    private String resource;

    @Column(name = "resource_id", length = 120)
    private String resourceId;

    @Column(nullable = false, length = 30)
    private String result;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(length = 1000)
    private String detail;

    /** Momento en que ocurrió la acción en el servicio emisor. */
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /** Momento en que audit-service recibió y registró el evento. */
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    /** Indica que el evento requiere atención de seguridad. */
    @Column(nullable = false)
    private boolean critical;

    /** Hash HMAC del evento anterior de la cadena. */
    @Column(name = "previous_hash", length = 64, updatable = false)
    private String previousHash;

    /** HMAC-SHA256 del contenido canónico del evento. */
    @Column(name = "event_hash", nullable = false, length = 64, unique = true, updatable = false)
    private String eventHash;
}
