package com.ecommerce.audit_service.dto;

import com.ecommerce.audit_service.model.AuditEvent;

import java.time.Instant;
import java.util.UUID;

/** Representación segura de un evento de auditoría. */
public record AuditEventResponse(
        UUID id,
        String userId,
        String userEmail,
        String sourceService,
        String action,
        String resource,
        String resourceId,
        String result,
        String ipAddress,
        String correlationId,
        String detail,
        Instant occurredAt,
        Instant recordedAt,
        boolean critical,
        String previousHash,
        String eventHash
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getUserId(),
                event.getUserEmail(),
                event.getSourceService(),
                event.getAction(),
                event.getResource(),
                event.getResourceId(),
                event.getResult(),
                event.getIpAddress(),
                event.getCorrelationId(),
                event.getDetail(),
                event.getOccurredAt(),
                event.getRecordedAt(),
                event.isCritical(),
                event.getPreviousHash(),
                event.getEventHash()
        );
    }
}
