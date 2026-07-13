package com.ecommerce.audit_service.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Sanitiza el campo detail antes de almacenarlo.
 *
 * Evita guardar accidentalmente:
 * - Tokens Bearer.
 * - Contraseñas.
 * - Secretos.
 * - Tokens.
 * - Cookies.
 * - CVV.
 * - Headers Authorization.
 */
@Service
public class AuditDetailSanitizer {

    private static final int MAX_LENGTH = 1000;

    /**
     * Detecta tokens enviados como:
     *
     * Bearer eyJhbGciOi...
     */
    private static final Pattern BEARER_PATTERN =
            Pattern.compile(
                    "(?i)Bearer\\s+[^\\s,;]+"
            );

    /**
     * Detecta asignaciones sensibles como:
     *
     * password=123
     * token: abc
     * secret = valor
     * cvv=123
     * cookie=valor
     * authorization=Bearer...
     */
    private static final Pattern SENSITIVE_ASSIGNMENT_PATTERN =
            Pattern.compile(
                    "(?i)"
                            + "\\b"
                            + "(password|passwd|pwd|token|secret|cvv|cookie|authorization)"
                            + "\\s*[:=]\\s*"
                            + "[^\\s,;]+"
            );

    /**
     * Sanitiza el detalle recibido.
     *
     * @param detail texto original.
     * @return texto limpio o null.
     */
    public String sanitize(
            String detail
    ) {

        if (detail == null || detail.isBlank()) {
            return null;
        }

        /*
         * Primero elimina espacios externos.
         *
         * Esto debe ocurrir antes de cortar a 1000 caracteres,
         * porque si se corta primero y después se hace trim(),
         * el resultado puede quedar en 998, como ocurrió en el test.
         */
        String sanitized = detail.trim();

        /*
         * Reemplaza cualquier token Bearer completo.
         */
        sanitized = BEARER_PATTERN
                .matcher(sanitized)
                .replaceAll("Bearer [REDACTED]");

        /*
         * Reemplaza asignaciones sensibles manteniendo
         * el nombre del campo.
         *
         * Ejemplo:
         * password=123 -> password=[REDACTED]
         */
        sanitized = SENSITIVE_ASSIGNMENT_PATTERN
                .matcher(sanitized)
                .replaceAll("$1=[REDACTED]");

        /*
         * Limita el contenido final a 1000 caracteres.
         *
         * El substring se aplica al final para garantizar
         * exactamente el límite máximo esperado.
         */
        if (sanitized.length() > MAX_LENGTH) {
            sanitized = sanitized.substring(
                    0,
                    MAX_LENGTH
            );
        }

        return sanitized;
    }
}