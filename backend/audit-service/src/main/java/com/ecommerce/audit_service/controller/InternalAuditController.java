package com.ecommerce.audit_service.controller;

import com.ecommerce.audit_service.dto.AuditEventResponse;
import com.ecommerce.audit_service.dto.CreateAuditEventRequest;
import com.ecommerce.audit_service.filter.ServiceTokenFilter;
import com.ecommerce.audit_service.service.AuditEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint interno de solo escritura para los demás microservicios. */
@RestController
@RequestMapping("/internal/audit-events")
public class InternalAuditController {

    private final AuditEventService auditEventService;

    public InternalAuditController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEventResponse create(
            @Valid @RequestBody CreateAuditEventRequest request,
            HttpServletRequest servletRequest
    ) {
        String authenticatedService = (String) servletRequest.getAttribute(
                ServiceTokenFilter.AUTHENTICATED_SERVICE_ATTRIBUTE
        );
        return auditEventService.create(request, authenticatedService);
    }
}
