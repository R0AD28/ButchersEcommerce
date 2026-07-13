package com.ecommerce.audit_service.service;

import com.ecommerce.audit_service.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Emite una alerta operativa sin imprimir secretos.
 * Después puede reemplazarse por una llamada a support-service.
 */
@Service
public class SecurityAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAlertService.class);

    public void alert(AuditEvent event) {
        if (!event.isCritical()) {
            return;
        }

        LOGGER.warn(
                "SECURITY_ALERT action={} service={} user={} ip={} correlationId={}",
                event.getAction(),
                event.getSourceService(),
                event.getUserEmail(),
                event.getIpAddress(),
                event.getCorrelationId()
        );
    }
}
