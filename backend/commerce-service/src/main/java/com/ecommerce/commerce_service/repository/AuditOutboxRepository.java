package com.ecommerce.commerce_service.repository;

import com.ecommerce.commerce_service.model.AuditOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditOutboxRepository extends JpaRepository<AuditOutbox, UUID> {
    List<AuditOutbox> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(AuditOutbox.Status status,
            Instant now);
}
