package com.ecommerce.auth_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH =
            "/auth/login";

    private static final String REGISTER_PATH =
            "/auth/register";

    private static final String REFRESH_PATH =
            "/auth/refresh";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final long loginLimit;

    private final long registerLimit;

    private final long refreshLimit;

    private final long windowSeconds;

    public RateLimitFilter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value(
                    "${security.rate-limit.login-requests:10}"
            )
            long loginLimit,
            @Value(
                    "${security.rate-limit.register-requests:5}"
            )
            long registerLimit,
            @Value(
                    "${security.rate-limit.refresh-requests:20}"
            )
            long refreshLimit,
            @Value(
                    "${security.rate-limit.window-seconds:60}"
            )
            long windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.loginLimit = loginLimit;
        this.registerLimit = registerLimit;
        this.refreshLimit = refreshLimit;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path = request.getRequestURI();

        return !LOGIN_PATH.equals(path)
                && !REGISTER_PATH.equals(path)
                && !REFRESH_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path =
                request.getRequestURI();

        long requestLimit =
                resolveLimit(path);

        String clientAddress =
                request.getRemoteAddr();

        String redisKey =
                buildRedisKey(
                        path,
                        clientAddress
                );

        Long currentRequests =
                redisTemplate
                        .opsForValue()
                        .increment(redisKey);

        if (currentRequests == null) {

            /*
             * Si Redis no devuelve un valor, no se bloquea
             * la operación principal.
             */
            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        if (currentRequests == 1) {

            redisTemplate.expire(
                    redisKey,
                    Duration.ofSeconds(
                            windowSeconds
                    )
            );
        }

        Long remainingTime =
                redisTemplate.getExpire(
                        redisKey
                );

        long retryAfter =
                remainingTime != null
                        && remainingTime > 0
                        ? remainingTime
                        : windowSeconds;

        response.setHeader(
                "X-RateLimit-Limit",
                String.valueOf(requestLimit)
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(
                        Math.max(
                                requestLimit
                                        - currentRequests,
                                0
                        )
                )
        );

        response.setHeader(
                "X-RateLimit-Reset",
                String.valueOf(retryAfter)
        );

        if (currentRequests > requestLimit) {

            writeTooManyRequestsResponse(
                    request,
                    response,
                    retryAfter
            );

            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    private long resolveLimit(
            String path
    ) {

        return switch (path) {

            case LOGIN_PATH ->
                    loginLimit;

            case REGISTER_PATH ->
                    registerLimit;

            case REFRESH_PATH ->
                    refreshLimit;

            default ->
                    Long.MAX_VALUE;
        };
    }

    private String buildRedisKey(
            String path,
            String clientAddress
    ) {

        String normalizedPath =
                path
                        .replace("/", ":")
                        .replaceAll("^:+", "");

        return "rate-limit:"
                + normalizedPath
                + ":"
                + clientAddress;
    }

    private void writeTooManyRequestsResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfter
    ) throws IOException {

        response.setStatus(429);

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding("UTF-8");

        response.setHeader(
                "Retry-After",
                String.valueOf(retryAfter)
        );

        RateLimitError error =
                new RateLimitError(
                        Instant.now(),
                        429,
                        "Too Many Requests",
                        "RATE_LIMIT_EXCEEDED",
                        "Se excedió el número permitido "
                                + "de solicitudes. Intente nuevamente "
                                + "en "
                                + retryAfter
                                + " segundos.",
                        request.getRequestURI(),
                        MDC.get("correlationId"),
                        Map.of()
                );

        objectMapper.writeValue(
                response.getOutputStream(),
                error
        );
    }

    private record RateLimitError(
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