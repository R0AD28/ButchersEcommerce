package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        ApiErrorResponse errorResponse =
                new ApiErrorResponse(
                        Instant.now(),
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized",
                        ErrorCode.INVALID_ACCESS_TOKEN.name(),
                        "El token de acceso no es válido o ha expirado",
                        request.getRequestURI(),
                        MDC.get("correlationId"),
                        Map.of()
                );

        objectMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }

    private record ApiErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String code,
            String message,
            String path,
            String correlationId,
            Map<String, String> validationErrors
    ) {
    }
}