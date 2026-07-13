package com.ecommerce.commerce_service.service;
import com.ecommerce.commerce_service.config.SecurityProperties;
import com.ecommerce.commerce_service.model.AuditOutbox;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.Instant;
@Service
public class AuditClient {
 private final RestClient restClient;
 private final SecurityProperties.Services.Audit properties;
 public AuditClient(RestClient.Builder builder, SecurityProperties securityProperties){
  this.properties=securityProperties.services().audit(); this.restClient=builder.baseUrl(properties.url()).build();
 }
 public void send(AuditOutbox event){
  restClient.post().uri("/internal/audit-events")
   .header("X-Service-Name",properties.serviceName()).header("X-Service-Token",properties.serviceToken())
   .header("X-Correlation-Id",event.getCorrelationId()==null?"":event.getCorrelationId())
   .body(new AuditEvent(event.getUserId(),event.getUserEmail(),"commerce-service",event.getAction(),
    event.getResourceType(),event.getResourceId(),event.getResult(),event.getIpAddress(),event.getDetails(),
    event.getCorrelationId(),event.getCreatedAt())).retrieve().toBodilessEntity();
 }
 private record AuditEvent(Long userId,String userEmail,String service,String action,String resourceType,
  String resourceId,String result,String ipAddress,String details,String correlationId,Instant occurredAt){}
}
