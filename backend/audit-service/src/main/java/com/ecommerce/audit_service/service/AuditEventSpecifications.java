package com.ecommerce.audit_service.service;

import com.ecommerce.audit_service.model.AuditEvent;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/** Filtros reutilizables para la consulta administrativa de auditorías. */
public final class AuditEventSpecifications {

    private AuditEventSpecifications() {
    }

    public static Specification<AuditEvent> userEmailEquals(String email) {
        return (root, query, cb) -> email == null || email.isBlank()
                ? cb.conjunction()
                : cb.equal(cb.lower(root.get("userEmail")), email.trim().toLowerCase());
    }

    public static Specification<AuditEvent> serviceEquals(String service) {
        return (root, query, cb) -> service == null || service.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("sourceService"), service.trim());
    }

    public static Specification<AuditEvent> actionEquals(String action) {
        return (root, query, cb) -> action == null || action.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("action"), action.trim());
    }

    public static Specification<AuditEvent> resultEquals(String result) {
        return (root, query, cb) -> result == null || result.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("result"), result.trim());
    }

    public static Specification<AuditEvent> criticalEquals(Boolean critical) {
        return (root, query, cb) -> critical == null
                ? cb.conjunction()
                : cb.equal(root.get("critical"), critical);
    }

    public static Specification<AuditEvent> occurredFrom(Instant from) {
        return (root, query, cb) -> from == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    public static Specification<AuditEvent> occurredUntil(Instant until) {
        return (root, query, cb) -> until == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("occurredAt"), until);
    }
}
