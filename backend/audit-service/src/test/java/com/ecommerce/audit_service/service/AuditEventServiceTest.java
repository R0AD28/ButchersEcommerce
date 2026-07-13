package com.ecommerce.audit_service.service;

import com.ecommerce.audit_service.dto.AuditEventResponse;
import com.ecommerce.audit_service.dto.CreateAuditEventRequest;
import com.ecommerce.audit_service.model.AuditEvent;
import com.ecommerce.audit_service.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock
    private AuditEventRepository repository;
    @Mock
    private AuditDetailSanitizer sanitizer;
    @Mock
    private SuspiciousActivityService suspiciousActivityService;
    @Mock
    private SecurityAlertService securityAlertService;
    @Mock
    private AuditIntegrityService integrityService;

    private AuditEventService service;

    @BeforeEach
    void setUp() {
        service = new AuditEventService(
                repository,
                sanitizer,
                suspiciousActivityService,
                securityAlertService,
                integrityService
        );
    }

    @Test
    void createNormalizesSanitizesHashesAndPersistsEvent() {
        CreateAuditEventRequest request = new CreateAuditEventRequest(
                " 99 ",
                " User@Example.COM ",
                " product_created ",
                " product ",
                " 51 ",
                " success ",
                " 10.0.0.5 ",
                " correlation-51 ",
                "token=very-secret",
                Instant.parse("2026-07-12T15:00:00Z")
        );

        when(suspiciousActivityService.isCritical(request)).thenReturn(false);
        when(sanitizer.sanitize(request.detail())).thenReturn("token=[REDACTED]");
        when(integrityService.currentPreviousHash()).thenReturn("0".repeat(64));
        when(integrityService.calculateHash(any(AuditEvent.class)))
                .thenReturn("a".repeat(64));
        when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        AuditEventResponse response = service.create(request, " catalog-service ");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        AuditEvent saved = captor.getValue();

        assertEquals("99", saved.getUserId());
        assertEquals("user@example.com", saved.getUserEmail());
        assertEquals("CATALOG-SERVICE", saved.getSourceService());
        assertEquals("PRODUCT_CREATED", saved.getAction());
        assertEquals("PRODUCT", saved.getResource());
        assertEquals("SUCCESS", saved.getResult());
        assertEquals("token=[REDACTED]", saved.getDetail());
        assertEquals("0".repeat(64), saved.getPreviousHash());
        assertEquals("a".repeat(64), saved.getEventHash());
        assertFalse(saved.isCritical());
        assertNotNull(saved.getRecordedAt());
        assertNotNull(response.id());
        verify(securityAlertService).alert(saved);
    }

    @Test
    void createMarksCriticalEvent() {
        CreateAuditEventRequest request = validRequest("ACCOUNT_LOCKED", "DENIED");
        when(suspiciousActivityService.isCritical(request)).thenReturn(true);
        when(integrityService.currentPreviousHash()).thenReturn("0".repeat(64));
        when(integrityService.calculateHash(any())).thenReturn("b".repeat(64));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEventResponse response = service.create(request, "auth-service");

        assertTrue(response.critical());
    }

    @Test
    void createRejectsUnsupportedResult() {
        CreateAuditEventRequest request = validRequest("LOGIN_SUCCESS", "OK");
        when(suspiciousActivityService.isCritical(request)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request, "auth-service")
        );

        assertTrue(exception.getMessage().contains("SUCCESS"));
    }

    @Test
    void createRejectsBlankAuthenticatedServiceName() {
        CreateAuditEventRequest request = validRequest("LOGIN_SUCCESS", "SUCCESS");
        when(suspiciousActivityService.isCritical(request)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request, "   ")
        );
    }

    private CreateAuditEventRequest validRequest(String action, String result) {
        return new CreateAuditEventRequest(
                "1",
                "user@example.com",
                action,
                "USER",
                "1",
                result,
                "10.0.0.1",
                "correlation-test",
                "detalle",
                Instant.parse("2026-07-12T15:00:00Z")
        );
    }
}
