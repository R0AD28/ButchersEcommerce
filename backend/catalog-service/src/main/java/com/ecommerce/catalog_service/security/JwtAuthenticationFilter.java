package com.ecommerce.catalog_service.security;

import com.ecommerce.catalog_service.dto.AuthorizationContext;
import com.ecommerce.catalog_service.service.AuthorizationClient;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthorizationClient authorizationClient;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AuthorizationClient authorizationClient
    ) {
        this.jwtService = jwtService;
        this.authorizationClient = authorizationClient;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            AuthorizationContext context = authorizationClient.getAuthorizationContext(token);

            if (!userId.equals(context.userId())
                    || !email.equalsIgnoreCase(context.email())
                    || !context.enabled()
                    || context.locked()) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            List<GrantedAuthority> authorities = new ArrayList<>();

            if (context.roles() != null) {
                context.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
            }

            if (context.permissions() != null) {
                context.permissions().stream()
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            authentication.setDetails(email);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return header.substring(BEARER_PREFIX.length());
    }
}
