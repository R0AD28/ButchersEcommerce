package com.ecommerce.audit_service.dto;

import java.time.Instant;
import java.util.UUID;

/** Resultado de comprobar la cadena criptográfica de auditoría. */
public record IntegrityVerificationResponse(
        boolean valid,
        long checkedEvents,
        UUID firstInvalidEventId,
        String message,
        Instant verifiedAt
) {
}
