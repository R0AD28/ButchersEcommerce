package com.ecommerce.audit_service.filter;

import com.ecommerce.audit_service.security.ServiceTokenValidator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Protege exclusivamente los endpoints /internal/**. */
@Component
public class ServiceTokenFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_SERVICE_ATTRIBUTE =
            "authenticatedServiceName";

    private final ServiceTokenValidator validator;

    public ServiceTokenFilter(ServiceTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String serviceName = request.getHeader("X-Service-Name");
        String serviceToken = request.getHeader("X-Service-Token");

        if (!validator.isValid(serviceName, serviceToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":401,"
                            + "\"error\":\"Unauthorized\","
                            + "\"message\":\"Credenciales internas inválidas\"}"
            );
            return;
        }

        // El controlador usa este atributo como fuente confiable del servicio.
        request.setAttribute(AUTHENTICATED_SERVICE_ATTRIBUTE, serviceName);
        filterChain.doFilter(request, response);
    }
}
