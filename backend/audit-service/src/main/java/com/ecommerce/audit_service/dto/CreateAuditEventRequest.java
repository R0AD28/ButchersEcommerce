package com.ecommerce.audit_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Cuerpo que envían auth-service, catalog-service y los demás servicios.
 * El nombre real del servicio NO se toma de este DTO, sino del header
 * X-Service-Name previamente validado.
 */
public record CreateAuditEventRequest(
        @Size(max = 100) String userId,
        @Size(max = 254) String userEmail,
        @NotBlank @Size(max = 120) String action,
        @Size(max = 120) String resource,
        @Size(max = 120) String resourceId,
        @NotBlank @Size(max = 30) String result,
        @Size(max = 64) String ipAddress,
        @Size(max = 100) String correlationId,
        @Size(max = 1000) String detail,
        @PastOrPresent Instant occurredAt
) {
}
