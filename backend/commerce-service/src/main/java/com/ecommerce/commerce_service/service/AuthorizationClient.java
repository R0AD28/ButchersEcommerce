package com.ecommerce.commerce_service.service;

import com.ecommerce.commerce_service.config.SecurityProperties;
import com.ecommerce.commerce_service.dto.AuthorizationContext;
import com.ecommerce.commerce_service.exception.BusinessException;
import com.ecommerce.commerce_service.exception.ErrorCode;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AuthorizationClient {

    private static final String CORRELATION_ID_KEY = "correlationId";

    private final RestClient restClient;
    private final SecurityProperties.Services.Auth authProperties;

    public AuthorizationClient(
            RestClient.Builder restClientBuilder,
            SecurityProperties securityProperties) {
        this.authProperties = securityProperties.services().auth();
        this.restClient = restClientBuilder
                .baseUrl(authProperties.url())
                .build();
    }

    public AuthorizationContext getAuthorizationContext(String jwt) {
        String correlationId = MDC.get(CORRELATION_ID_KEY);

        try {
            AuthorizationContext context = restClient.get()
                    .uri(authProperties.authorizationPath())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header("X-Service-Name", authProperties.serviceName())
                    .header("X-Service-Token", authProperties.serviceToken())
                    .header(
                            "X-Correlation-Id",
                            correlationId != null ? correlationId : "")
                    .retrieve()
                    .body(AuthorizationContext.class);

            if (context == null) {
                throw unavailable();
            }

            return context;

        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.AUTHORIZATION_SERVICE_UNAVAILABLE,
                "No fue posible validar los permisos del usuario");
    }
}
