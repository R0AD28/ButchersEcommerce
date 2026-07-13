package com.ecommerce.audit_service.service;

import com.ecommerce.audit_service.dto.AuthorizationContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Consulta a auth-service los roles y permisos vigentes del usuario.
 * Reenvía el JWT de identidad y agrega credenciales internas del servicio.
 */
@Component
public class AuthorizationClient {

    private final RestClient restClient;
    private final String serviceName;
    private final String serviceToken;

    public AuthorizationClient(
            RestClient.Builder builder,
            @Value("${services.auth.base-url}") String authBaseUrl,
            @Value("${security.service.name}") String serviceName,
            @Value("${security.service.token}") String serviceToken
    ) {
        this.restClient = builder.baseUrl(authBaseUrl).build();
        this.serviceName = serviceName;
        this.serviceToken = serviceToken;
    }

    public AuthorizationContext getCurrentAuthorization(String bearerToken) {
        return restClient.get()
                .uri("/internal/authorization/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .header("X-Service-Name", serviceName)
                .header("X-Service-Token", serviceToken)
                .retrieve()
                .body(AuthorizationContext.class);
    }
}
