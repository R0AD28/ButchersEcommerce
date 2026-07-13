package com.ecommerce.commerce_service.service;

import com.ecommerce.commerce_service.model.AuditOutbox;
import com.ecommerce.commerce_service.repository.AuditOutboxRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuditOutboxService {
  private final AuditOutboxRepository repository;

  public AuditOutboxService(AuditOutboxRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void enqueue(Long userId, String email, String action, String resourceType, String resourceId,
      String result, String ip, String details) {
    save(userId, email, action, resourceType, resourceId, result, ip, details);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void enqueueRequiresNew(Long userId, String email, String action, String resourceType, String resourceId,
      String result, String ip, String details) {
    save(userId, email, action, resourceType, resourceId, result, ip, details);
  }

  private void save(Long userId, String email, String action, String resourceType, String resourceId,
      String result, String ip, String details) {
    Instant now = Instant.now();
    repository.save(AuditOutbox.builder().id(UUID.randomUUID()).userId(userId).userEmail(email)
        .action(action).resourceType(resourceType).resourceId(resourceId).result(result).ipAddress(ip)
        .details(details).correlationId(MDC.get("correlationId")).status(AuditOutbox.Status.PENDING)
        .attempts(0).nextAttemptAt(now).createdAt(now).build());
  }
}
