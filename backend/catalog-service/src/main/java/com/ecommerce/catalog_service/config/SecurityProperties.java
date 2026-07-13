package com.ecommerce.catalog_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        Jwt jwt,
        Cors cors,
        Services services,
        RateLimit rateLimit,
        Idempotency idempotency,
        HttpClient httpClient
) {
    public record Jwt(String publicKeyPath, String issuer, String audience) {}
    public record Cors(String allowedOrigins) {}
    public record RateLimit(int readRequests, int writeRequests, int windowSeconds) {}
    public record Idempotency(long ttlHours) {}
    public record HttpClient(int connectTimeoutMillis, int readTimeoutMillis) {}

    public record Services(Auth auth, Audit audit) {
        public record Auth(String url, String authorizationPath, String serviceName, String serviceToken) {}
        public record Audit(String url, String serviceName, String serviceToken) {}
    }
}
