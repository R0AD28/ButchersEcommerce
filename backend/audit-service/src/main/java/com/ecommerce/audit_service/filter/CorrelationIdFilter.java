package com.ecommerce.audit_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Garantiza que cada solicitud tenga un identificador de correlación.
 *
 * Este identificador permite relacionar:
 * - La solicitud HTTP.
 * - Los logs del servicio.
 * - Los eventos de auditoría.
 * - Las llamadas entre microservicios.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";

    /*
     * Nombre del atributo donde se guarda el correlationId
     * dentro de la solicitud HTTP.
     *
     * Otros componentes, como los controladores, pueden recuperarlo
     * mediante request.getAttribute(REQUEST_ATTRIBUTE).
     */
    public static final String REQUEST_ATTRIBUTE = "correlationId";

    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId =
                request.getHeader(HEADER_NAME);

        /*
         * Si el cliente o microservicio no envía un correlationId,
         * se genera uno nuevo.
         */
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            correlationId = correlationId.trim();
        }

        /*
         * Guarda el identificador como atributo de la petición.
         * Así puede recuperarse desde controladores y servicios.
         */
        request.setAttribute(
                REQUEST_ATTRIBUTE,
                correlationId
        );

        /*
         * Devuelve el mismo identificador en la respuesta.
         */
        response.setHeader(
                HEADER_NAME,
                correlationId
        );

        /*
         * Lo incorpora al contexto de logging.
         */
        MDC.put(
                MDC_KEY,
                correlationId
        );

        try {
            filterChain.doFilter(
                    request,
                    response
            );
        } finally {
            /*
             * Limpia el MDC para evitar que el correlationId
             * se reutilice accidentalmente en otra petición.
             */
            MDC.remove(MDC_KEY);
        }
    }
}