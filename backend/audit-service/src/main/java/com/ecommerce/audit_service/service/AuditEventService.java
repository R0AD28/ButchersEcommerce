package com.ecommerce.audit_service.service;

import com.ecommerce.audit_service.dto.AuditEventResponse;
import com.ecommerce.audit_service.dto.CreateAuditEventRequest;
import com.ecommerce.audit_service.model.AuditEvent;
import com.ecommerce.audit_service.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Lógica de persistencia, consulta y autopista de auditoría. */
@Service
public class AuditEventService {

    private static final Set<String> ALLOWED_RESULTS = Set.of(
            "SUCCESS", "FAILURE", "DENIED", "ERROR"
    );

    private final AuditEventRepository repository;
    private final AuditDetailSanitizer detailSanitizer;
    private final SuspiciousActivityService suspiciousActivityService;
    private final SecurityAlertService securityAlertService;
    private final AuditIntegrityService integrityService;
    private final Clock clock;

    public AuditEventService(
            AuditEventRepository repository,
            AuditDetailSanitizer detailSanitizer,
            SuspiciousActivityService suspiciousActivityService,
            SecurityAlertService securityAlertService,
            AuditIntegrityService integrityService
    ) {
        this.repository = repository;
        this.detailSanitizer = detailSanitizer;
        this.suspiciousActivityService = suspiciousActivityService;
        this.securityAlertService = securityAlertService;
        this.integrityService = integrityService;
        this.clock = Clock.systemUTC();
    }

    /** Crea un evento usando el nombre del servicio interno ya autenticado. */
    @Transactional
    public synchronized AuditEventResponse create(
            CreateAuditEventRequest request,
            String authenticatedServiceName
    ) {
        boolean critical = suspiciousActivityService.isCritical(request);
        AuditEvent event = buildEvent(
                request,
                normalizeRequired(authenticatedServiceName),
                critical
        );

        AuditEvent saved = repository.save(event);
        securityAlertService.alert(saved);
        return AuditEventResponse.from(saved);
    }

    /**
     * Consulta paginada y registra quién accedió a los logs.
     * No llama de nuevo al método search, por lo que no genera recursión.
     */
    @Transactional
    public synchronized Page<AuditEventResponse> searchAndAudit(
            String userEmail,
            String sourceService,
            String action,
            String result,
            Boolean critical,
            Instant from,
            Instant until,
            Pageable pageable,
            String queryingUserEmail,
            String queryingIp,
            String correlationId
    ) {
        Specification<AuditEvent> specification = Specification.allOf(
                AuditEventSpecifications.userEmailEquals(normalizeEmail(userEmail)),
                AuditEventSpecifications.serviceEquals(normalizeOptional(sourceService)),
                AuditEventSpecifications.actionEquals(normalizeOptional(action)),
                AuditEventSpecifications.resultEquals(normalizeOptional(result)),
                AuditEventSpecifications.criticalEquals(critical),
                AuditEventSpecifications.occurredFrom(from),
                AuditEventSpecifications.occurredUntil(until)
        );

        Page<AuditEventResponse> response = repository
                .findAll(specification, pageable)
                .map(AuditEventResponse::from);

        CreateAuditEventRequest queryAudit = new CreateAuditEventRequest(
                null,
                queryingUserEmail,
                "AUDIT_LOGS_VIEWED",
                "AUDIT_EVENT",
                null,
                "SUCCESS",
                queryingIp,
                correlationId,
                "page=" + pageable.getPageNumber()
                        + ",size=" + pageable.getPageSize()
                        + ",returned=" + response.getNumberOfElements(),
                clock.instant()
        );
        repository.save(buildEvent(queryAudit, "AUDIT-SERVICE", false));

        return response;
    }

    private AuditEvent buildEvent(
            CreateAuditEventRequest request,
            String sourceService,
            boolean critical
    ) {
        Instant occurredAt = request.occurredAt() == null
                ? clock.instant()
                : request.occurredAt();
        Instant recordedAt = clock.instant();

        String result = normalizeRequired(request.result());
        if (!ALLOWED_RESULTS.contains(result)) {
            throw new IllegalArgumentException(
                    "result debe ser SUCCESS, FAILURE, DENIED o ERROR"
            );
        }

        String correlationId = request.correlationId() == null
                || request.correlationId().isBlank()
                ? UUID.randomUUID().toString()
                : request.correlationId().trim();

        AuditEvent event = AuditEvent.builder()
                .userId(clean(request.userId()))
                .userEmail(normalizeEmail(request.userEmail()))
                .sourceService(sourceService)
                .action(normalizeRequired(request.action()))
                .resource(normalizeOptional(request.resource()))
                .resourceId(clean(request.resourceId()))
                .result(result)
                .ipAddress(clean(request.ipAddress()))
                .correlationId(correlationId)
                .detail(detailSanitizer.sanitize(request.detail()))
                .occurredAt(occurredAt)
                .recordedAt(recordedAt)
                .critical(critical)
                .previousHash(integrityService.currentPreviousHash())
                .build();

        event.setEventHash(integrityService.calculateHash(event));
        return event;
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank()
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El valor obligatorio no puede estar vacío");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
