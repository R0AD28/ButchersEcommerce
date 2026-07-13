package com.ecommerce.audit_service.controller;

import com.ecommerce.audit_service.dto.AuditEventResponse;
import com.ecommerce.audit_service.dto.IntegrityVerificationResponse;
import com.ecommerce.audit_service.filter.CorrelationIdFilter;
import com.ecommerce.audit_service.service.AuditEventService;
import com.ecommerce.audit_service.service.AuditIntegrityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditQueryControllerTest {

    @Mock
    private AuditEventService auditEventService;
    @Mock
    private AuditIntegrityService integrityService;

    @Test
    void searchNormalizesPageAndLimitsMaximumSize() {
        AuditQueryController controller = new AuditQueryController(
                auditEventService,
                integrityService
        );
        Page<AuditEventResponse> expected = new PageImpl<>(List.of());
        when(auditEventService.searchAndAudit(
                any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any()
        )).thenReturn(expected);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.setAttribute(
                CorrelationIdFilter.REQUEST_ATTRIBUTE,
                "correlation-query"
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "auditor@example.com",
                        null,
                        List.of()
                );

        Page<AuditEventResponse> actual = controller.search(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                -5,
                500,
                authentication,
                request
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditEventService).searchAndAudit(
                eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), pageableCaptor.capture(),
                eq("auditor@example.com"),
                eq("10.0.0.5"),
                eq("correlation-query")
        );
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertSame(expected, actual);
    }

    @Test
    void verifyIntegrityDelegatesToIntegrityService() {
        AuditQueryController controller = new AuditQueryController(
                auditEventService,
                integrityService
        );
        IntegrityVerificationResponse expected = new IntegrityVerificationResponse(
                true,
                5,
                null,
                "Cadena de auditoría íntegra",
                Instant.parse("2026-07-12T16:00:00Z")
        );
        when(integrityService.verifyChain()).thenReturn(expected);

        IntegrityVerificationResponse actual = controller.verifyIntegrity();

        assertSame(expected, actual);
        verify(integrityService).verifyChain();
    }
}
