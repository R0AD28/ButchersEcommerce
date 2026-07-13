package com.ecommerce.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(

        Jwt jwt,
        Login login,
        RefreshToken refreshToken,
        Cookie cookie,
        AuthorizationCache authorizationCache,
        InternalServices internalServices,
        Services services
) {

    public record Jwt(
            String privateKeyPath,
            String publicKeyPath,
            String issuer,
            String audience,
            long accessTokenExpirationSeconds
    ) {
    }

    public record Login(
            int maxAttempts,
            long lockDurationMinutes
    ) {
    }

    public record RefreshToken(
            long expirationDays
    ) {
    }

    public record Cookie(
            boolean secure,
            String sameSite,
            String path
    ) {
    }

    public record AuthorizationCache(
            long ttlSeconds
    ) {
    }

    public record InternalServices(
            Map<String, String> tokens
    ) {
    }

    public record Services(
            Audit audit
    ) {

        public record Audit(
                String url,
                String name,
                String token
        ) {
        }
    }
}