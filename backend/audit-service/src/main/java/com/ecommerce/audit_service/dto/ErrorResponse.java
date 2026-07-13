package com.ecommerce.audit_service.dto;

import java.time.Instant;

/** Respuesta JSON uniforme para errores de la API. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
) {
}
