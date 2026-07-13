package com.ecommerce.audit_service.service;

import com.ecommerce.audit_service.dto.CreateAuditEventRequest;
import com.ecommerce.audit_service.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuspiciousActivityServiceTest {

    @Mock
    private AuditEventRepository repository;

    private SuspiciousActivityService service;

    @BeforeEach
    void setUp() {
        service = new SuspiciousActivityService(repository, 5, 5);
    }

    @Test
    void isCriticalReturnsTrueForAlwaysCriticalAction() {
        CreateAuditEventRequest request = request(
                "ROLE_REPLACED",
                "admin@example.com",
                "10.0.0.1"
        );

        assertTrue(service.isCritical(request));
    }

    @Test
    void isCriticalReturnsTrueWhenLoginFailureThresholdIsReached() {
        when(repository.countByActionAndUserEmailAndOccurredAtAfter(
                eq("LOGIN_FAILED"),
                eq("user@example.com"),
                any(Instant.class)
        )).thenReturn(4L);

        boolean critical = service.isCritical(request(
                "login_failed",
                " USER@EXAMPLE.COM ",
                "10.0.0.1"
        ));

        assertTrue(critical);
        verify(repository).countByActionAndUserEmailAndOccurredAtAfter(
                eq("LOGIN_FAILED"),
                eq("user@example.com"),
                any(Instant.class)
        );
    }

    @Test
    void isCriticalReturnsFalseWhenThresholdIsNotReached() {
        when(repository.countByActionAndIpAddressAndOccurredAtAfter(
                eq("INVALID_TOKEN"),
                eq("10.0.0.9"),
                any(Instant.class)
        )).thenReturn(2L);

        assertFalse(service.isCritical(request(
                "INVALID_TOKEN",
                null,
                " 10.0.0.9 "
        )));
    }

    private CreateAuditEventRequest request(
            String action,
            String email,
            String ipAddress
    ) {
        return new CreateAuditEventRequest(
                "15",
                email,
                action,
                "USER",
                "15",
                "FAILURE",
                ipAddress,
                "correlation-test",
                "detalle",
                Instant.parse("2026-07-12T15:00:00Z")
        );
    }
}
