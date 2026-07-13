package com.ecommerce.audit_service.controller;

import com.ecommerce.audit_service.dto.AuditEventResponse;
import com.ecommerce.audit_service.dto.IntegrityVerificationResponse;
import com.ecommerce.audit_service.filter.CorrelationIdFilter;
import com.ecommerce.audit_service.service.AuditEventService;
import com.ecommerce.audit_service.service.AuditIntegrityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Consultas sensibles para auditores autorizados. */
@RestController
@RequestMapping("/audit-events")
public class AuditQueryController {

    private static final int MAX_PAGE_SIZE = 100;
    private final AuditEventService auditEventService;
    private final AuditIntegrityService integrityService;

    public AuditQueryController(
            AuditEventService auditEventService,
            AuditIntegrityService integrityService
    ) {
        this.auditEventService = auditEventService;
        this.integrityService = integrityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    public Page<AuditEventResponse> search(
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Boolean critical,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant until,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            HttpServletRequest request
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "occurredAt")
        );

        return auditEventService.searchAndAudit(
                userEmail,
                sourceService,
                action,
                result,
                critical,
                from,
                until,
                pageable,
                authentication.getName(),
                request.getRemoteAddr(),
                (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)
        );
    }

    @GetMapping("/integrity")
    @PreAuthorize("hasAuthority('VERIFY_AUDIT_INTEGRITY')")
    public IntegrityVerificationResponse verifyIntegrity() {
        return integrityService.verifyChain();
    }
}
