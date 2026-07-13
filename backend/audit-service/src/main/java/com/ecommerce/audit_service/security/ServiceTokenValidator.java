package com.ecommerce.audit_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Valida qué microservicios pueden insertar auditorías.
 * La comparación constante reduce filtraciones por tiempo de respuesta.
 */
@Component
public class ServiceTokenValidator {

    private final Map<String, String> allowedServices;

    public ServiceTokenValidator(
            @Value("${security.internal-services.auth-service-token}") String authToken,
            @Value("${security.internal-services.catalog-service-token}") String catalogToken,
            @Value("${security.internal-services.commerce-service-token}") String commerceToken    ) {
        this.allowedServices = Map.of(
                "auth-service", authToken,
                "catalog-service", catalogToken,
                "commerce-service", commerceToken
        );
    }

    public boolean isValid(String serviceName, String providedToken) {
        if (serviceName == null || providedToken == null) {
            return false;
        }

        String expectedToken = allowedServices.get(serviceName);
        if (expectedToken == null || expectedToken.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
