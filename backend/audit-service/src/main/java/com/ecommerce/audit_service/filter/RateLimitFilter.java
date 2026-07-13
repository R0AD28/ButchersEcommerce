package com.ecommerce.audit_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Límite sencillo por minuto; en producción debe reforzarse en el gateway. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final int internalLimit;
    private final int queryLimit;
    private final Clock clock = Clock.systemUTC();

    public RateLimitFilter(
            @Value("${security.rate-limit.internal-per-minute:300}") int internalLimit,
            @Value("${security.rate-limit.query-per-minute:30}") int queryLimit
    ) {
        this.internalLimit = internalLimit;
        this.queryLimit = queryLimit;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit;
        String key;

        if (path.startsWith("/internal/audit-events")) {
            limit = internalLimit;
            key = "service:" + request.getHeader("X-Service-Name");
        } else if (path.startsWith("/audit-events")) {
            limit = queryLimit;
            key = "ip:" + request.getRemoteAddr();
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        if (!allow(key, limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"RATE_LIMIT_EXCEEDED\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean allow(String key, int limit) {
        long minute = clock.instant().getEpochSecond() / 60;
        WindowCounter counter = counters.compute(key, (ignored, current) -> {
            if (current == null || current.minute != minute) {
                return new WindowCounter(minute, new AtomicInteger(1));
            }
            current.count.incrementAndGet();
            return current;
        });
        return counter.count.get() <= limit;
    }

    private record WindowCounter(long minute, AtomicInteger count) {
    }
}
