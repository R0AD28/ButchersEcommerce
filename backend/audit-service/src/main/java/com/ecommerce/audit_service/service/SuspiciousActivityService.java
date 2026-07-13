package com.ecommerce.audit_service.service;

import com.ecommerce.audit_service.dto.CreateAuditEventRequest;
import com.ecommerce.audit_service.repository.AuditEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/** Clasifica eventos críticos y detecta repeticiones dentro de una ventana. */
@Service
public class SuspiciousActivityService {

    private static final Set<String> ALWAYS_CRITICAL_ACTIONS = Set.of(
            "ACCOUNT_LOCKED",
            "ROLE_ASSIGNED",
            "ROLE_REPLACED",
            "INVALID_SERVICE_TOKEN",
            "AUDIT_INTEGRITY_FAILURE",
            "PAYMENT_TAMPERING_ATTEMPT"
    );

    private final AuditEventRepository repository;
    private final Clock clock;
    private final int failureThreshold;
    private final Duration detectionWindow;

    public SuspiciousActivityService(
            AuditEventRepository repository,
            @Value("${security.audit.failure-threshold:5}") int failureThreshold,
            @Value("${security.audit.detection-window-minutes:5}") long windowMinutes
    ) {
        this.repository = repository;
        this.clock = Clock.systemUTC();
        this.failureThreshold = failureThreshold;
        this.detectionWindow = Duration.ofMinutes(windowMinutes);
    }

    public boolean isCritical(CreateAuditEventRequest request) {
        String action = normalize(request.action());
        if (ALWAYS_CRITICAL_ACTIONS.contains(action)) {
            return true;
        }

        if ("LOGIN_FAILED".equals(action) && request.userEmail() != null) {
            long previousFailures = repository
                    .countByActionAndUserEmailAndOccurredAtAfter(
                            action,
                            request.userEmail().trim().toLowerCase(Locale.ROOT),
                            clock.instant().minus(detectionWindow)
                    );
            return previousFailures + 1 >= failureThreshold;
        }

        if (("INVALID_TOKEN".equals(action) || "ACCESS_DENIED".equals(action))
                && request.ipAddress() != null) {
            long previousFailures = repository
                    .countByActionAndIpAddressAndOccurredAtAfter(
                            action,
                            request.ipAddress().trim(),
                            clock.instant().minus(detectionWindow)
                    );
            return previousFailures + 1 >= failureThreshold;
        }

        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
