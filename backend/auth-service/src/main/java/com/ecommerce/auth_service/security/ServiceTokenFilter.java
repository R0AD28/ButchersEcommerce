package com.ecommerce.auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ServiceTokenFilter extends OncePerRequestFilter {

    public static final String SERVICE_NAME_HEADER =
            "X-Service-Name";

    public static final String SERVICE_TOKEN_HEADER =
            "X-Service-Token";

    private final ServiceTokenValidator serviceTokenValidator;

    public ServiceTokenFilter(
            ServiceTokenValidator serviceTokenValidator
    ) {
        this.serviceTokenValidator =
                serviceTokenValidator;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        return !request
                .getRequestURI()
                .startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String serviceName =
                request.getHeader(
                        SERVICE_NAME_HEADER
                );

        String serviceToken =
                request.getHeader(
                        SERVICE_TOKEN_HEADER
                );

        if (
                !serviceTokenValidator.isValid(
                        serviceName,
                        serviceToken
                )
        ) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            response.getWriter().write(
                    """
                    {
                      "status": 401,
                      "error": "Unauthorized",
                      "code": "INVALID_SERVICE_TOKEN",
                      "message": "El token interno del servicio no es válido"
                    }
                    """
            );

            return;
        }

        request.setAttribute(
                "authenticatedService",
                serviceName
        );

        filterChain.doFilter(request, response);
    }
}