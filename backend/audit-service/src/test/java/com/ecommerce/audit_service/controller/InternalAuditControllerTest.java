package com.ecommerce.audit_service.controller;

import com.ecommerce.audit_service.dto.AuditEventResponse;
import com.ecommerce.audit_service.dto.CreateAuditEventRequest;
import com.ecommerce.audit_service.filter.ServiceTokenFilter;
import com.ecommerce.audit_service.service.AuditEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalAuditControllerTest {

    @Mock
    private AuditEventService auditEventService;

    @Test
    void createUsesAuthenticatedServiceStoredByFilter() {
        InternalAuditController controller = new InternalAuditController(auditEventService);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(
                ServiceTokenFilter.AUTHENTICATED_SERVICE_ATTRIBUTE,
                "catalog-service"
        );
        CreateAuditEventRequest request = new CreateAuditEventRequest(
                "1",
                "user@example.com",
                "PRODUCT_CREATED",
                "PRODUCT",
                "10",
                "SUCCESS",
                "10.0.0.1",
                "correlation-1",
                "detalle",
                Instant.parse("2026-07-12T15:00:00Z")
        );
        AuditEventResponse expected = new AuditEventResponse(
                UUID.randomUUID(),
                "1",
                "user@example.com",
                "CATALOG-SERVICE",
                "PRODUCT_CREATED",
                "PRODUCT",
                "10",
                "SUCCESS",
                "10.0.0.1",
                "correlation-1",
                "detalle",
                request.occurredAt(),
                request.occurredAt(),
                false,
                "0".repeat(64),
                "a".repeat(64)
        );
        when(auditEventService.create(request, "catalog-service"))
                .thenReturn(expected);

        AuditEventResponse actual = controller.create(request, servletRequest);

        assertSame(expected, actual);
        verify(auditEventService).create(request, "catalog-service");
    }
}
