package com.ecommerce.audit_service.repository;

import com.ecommerce.audit_service.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Acceso append-only a los eventos y consultas de detección. */
public interface AuditEventRepository extends
        JpaRepository<AuditEvent, UUID>,
        JpaSpecificationExecutor<AuditEvent> {

    Optional<AuditEvent> findFirstByOrderByRecordedAtDescIdDesc();

    List<AuditEvent> findAllByOrderByRecordedAtAscIdAsc();

    long countByActionAndUserEmailAndOccurredAtAfter(
            String action,
            String userEmail,
            Instant occurredAt
    );

    long countByActionAndIpAddressAndOccurredAtAfter(
            String action,
            String ipAddress,
            Instant occurredAt
    );
}
