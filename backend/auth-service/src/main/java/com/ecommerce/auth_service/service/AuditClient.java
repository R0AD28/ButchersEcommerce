package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
public class AuditClient {

    private static final Logger log =
            LoggerFactory.getLogger(AuditClient.class);

    private static final String CORRELATION_ID_KEY =
            "correlationId";

    private final RestClient restClient;

    private final SecurityProperties.Services.Audit auditProperties;

    public AuditClient(
            RestClient.Builder restClientBuilder,
            SecurityProperties securityProperties
    ) {

        this.auditProperties =
                securityProperties.services().audit();

        this.restClient = restClientBuilder
                .baseUrl(auditProperties.url())
                .build();
    }

    public void send(
            Long userId,
            String userEmail,
            String action,
            String resourceType,
            String resourceId,
            String result,
            String ipAddress,
            String details
    ) {

        String correlationId =
                MDC.get(CORRELATION_ID_KEY);

        try {

            restClient.post()
                    .uri("/internal/audit-events")
                    .header(
                            "X-Service-Name",
                            auditProperties.name()
                    )
                    .header(
                            "X-Service-Token",
                            auditProperties.token()
                    )
                    .header(
                            "X-Correlation-Id",
                            correlationId != null
                                    ? correlationId
                                    : ""
                    )
                    .body(
                            new AuditEvent(
                                    userId,
                                    userEmail,
                                    "auth-service",
                                    action,
                                    resourceType,
                                    resourceId,
                                    result,
                                    ipAddress,
                                    details,
                                    correlationId,
                                    Instant.now()
                            )
                    )
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception exception) {

            /*
             * Una caída de audit-service no debe impedir
             * que la operación principal finalice.
             */
            log.error(
                    "No se pudo registrar la auditoría. "
                            + "action={}, correlationId={}",
                    action,
                    correlationId,
                    exception
            );
        }
    }

    private record AuditEvent(
            Long userId,
            String userEmail,
            String service,
            String action,
            String resourceType,
            String resourceId,
            String result,
            String ipAddress,
            String details,
            String correlationId,
            Instant occurredAt
    ) {
    }
}