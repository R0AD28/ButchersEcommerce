package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.exception.ApiErrorResponse;
import com.ecommerce.auth_service.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    public RestAccessDeniedHandler(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                Instant.now(), 403, "Forbidden", ErrorCode.INSUFFICIENT_PERMISSION.name(),
                "No tiene permisos para realizar esta operación", request.getRequestURI(),
                MDC.get("correlationId"), Map.of()
        ));
    }
}
