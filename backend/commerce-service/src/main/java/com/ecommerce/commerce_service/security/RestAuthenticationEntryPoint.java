package com.ecommerce.commerce_service.security;

import com.ecommerce.commerce_service.exception.ErrorCode;
import com.ecommerce.commerce_service.service.AuditOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;
    private final AuditOutboxService audit;

    public RestAuthenticationEntryPoint(ObjectMapper mapper, AuditOutboxService audit) {
        this.mapper = mapper;
        this.audit = audit;
    }

    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        try {
            audit.enqueueRequiresNew(null, null, "AUTHENTICATION_FAILED", "HTTP", req.getRequestURI(), "FAILURE",
                    req.getRemoteAddr(), req.getMethod());
        } catch (RuntimeException ignored) {
        }
        res.setStatus(401);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getOutputStream(),
                new SecurityError(Instant.now(), 401, "Unauthorized", ErrorCode.INVALID_ACCESS_TOKEN.name(),
                        "El token de acceso no es válido o ha expirado", req.getRequestURI(), MDC.get("correlationId"),
                        Map.of()));
    }

    private record SecurityError(Instant timestamp, int status, String error, String code, String message, String path,
            String correlationId, Map<String, String> validationErrors) {
    }
}
