package com.ecommerce.catalog_service.filter;

import com.ecommerce.catalog_service.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties properties;

    public RateLimitFilter(StringRedisTemplate redisTemplate, SecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/products")
                && !request.getRequestURI().startsWith("/inventory");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean read = "GET".equalsIgnoreCase(request.getMethod());
        int limit = read ? properties.rateLimit().readRequests() : properties.rateLimit().writeRequests();
        int window = properties.rateLimit().windowSeconds();
        String key = "rate-limit:catalog:" + request.getMethod() + ":" + request.getRequestURI()
                + ":" + request.getRemoteAddr();
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(window));
            }
            long remaining = Math.max(0, limit - (count == null ? 0 : count));
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(Instant.now().getEpochSecond() + window));
            if (count != null && count > limit) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Retry-After", String.valueOf(window));
                response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Demasiadas solicitudes\"}");
                return;
            }
        } catch (RuntimeException exception) {
            log.error("Redis no está disponible para rate limiting", exception);
            if (!read) {
                response.setStatus(503);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"code\":\"RATE_LIMIT_SERVICE_UNAVAILABLE\",\"message\":\"No se puede validar el límite de solicitudes\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
