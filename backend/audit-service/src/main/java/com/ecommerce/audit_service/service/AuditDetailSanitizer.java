package com.ecommerce.audit_service.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Elimina secretos comunes antes de guardar texto libre en detail. */
@Component
public class AuditDetailSanitizer {

    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)bearer\\s+[a-z0-9._~+/=-]+"
    );
    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
            "(?i)(password|passwd|token|secret|authorization|cookie|cvv)\\s*[:=]\\s*([^,;\\s]+)"
    );

    public String sanitize(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }

        String sanitized = BEARER_TOKEN.matcher(detail)
                .replaceAll("Bearer [REDACTED]");
        sanitized = SENSITIVE_ASSIGNMENT.matcher(sanitized)
                .replaceAll("$1=[REDACTED]");

        return sanitized.length() <= 1000
                ? sanitized.trim()
                : sanitized.substring(0, 1000).trim();
    }
}
