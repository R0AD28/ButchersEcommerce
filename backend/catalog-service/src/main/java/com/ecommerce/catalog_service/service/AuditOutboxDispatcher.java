package com.ecommerce.catalog_service.service;
import com.ecommerce.catalog_service.model.AuditOutbox;
import com.ecommerce.catalog_service.repository.AuditOutboxRepository;
import org.slf4j.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
@Service
public class AuditOutboxDispatcher {
 private static final Logger log=LoggerFactory.getLogger(AuditOutboxDispatcher.class);
 private final AuditOutboxRepository repository; private final AuditClient client;
 public AuditOutboxDispatcher(AuditOutboxRepository repository,AuditClient client){this.repository=repository;this.client=client;}
 @Scheduled(fixedDelayString="${security.services.audit.outbox-delay-ms:5000}")
 @Transactional
 public void dispatch(){
  for(AuditOutbox event:repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(AuditOutbox.Status.PENDING,Instant.now())){
   try{client.send(event);event.setStatus(AuditOutbox.Status.SENT);event.setSentAt(Instant.now());}
   catch(RuntimeException ex){event.setAttempts(event.getAttempts()+1);long delay=Math.min(300,event.getAttempts()*10L);
    event.setNextAttemptAt(Instant.now().plusSeconds(delay));log.warn("No se pudo enviar auditoría {}",event.getId());}
  }
 }
}
