package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.dto.AuthorizationContext;
import com.ecommerce.auth_service.service.AuthorizationService;
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

    private static final String AUTHORIZATION_HEADER =
            "Authorization";

    private static final String BEARER_PREFIX =
            "Bearer ";

    private final JwtService jwtService;

    private final AuthorizationService authorizationService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AuthorizationService authorizationService
    ) {
        this.jwtService = jwtService;
        this.authorizationService = authorizationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (
                token == null ||
                SecurityContextHolder
                        .getContext()
                        .getAuthentication() != null
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            Long userId =
                    jwtService.extractUserId(token);

            String email =
                    jwtService.extractEmail(token);

            AuthorizationContext authorizationContext =
                    authorizationService
                            .getAuthorizationContext(userId);

            if (
                    !email.equalsIgnoreCase(
                            authorizationContext.email()
                    )
            ) {
                throw new JwtException(
                        "La identidad del JWT no coincide con el usuario"
                );
            }

            if (
                    !authorizationContext.enabled() ||
                    authorizationContext.locked()
            ) {
                filterChain.doFilter(request, response);
                return;
            }

            List<GrantedAuthority> authorities =
                    new ArrayList<>();

            authorizationContext.roles()
                    .stream()
                    .map(role ->
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role
                            )
                    )
                    .forEach(authorities::add);

            authorizationContext.permissions()
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            authentication.setDetails(email);

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (
                JwtException |
                IllegalArgumentException exception
        ) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(
            HttpServletRequest request
    ) {

        String authorizationHeader =
                request.getHeader(
                        AUTHORIZATION_HEADER
                );

        if (
                !StringUtils.hasText(authorizationHeader) ||
                !authorizationHeader.startsWith(
                        BEARER_PREFIX
                )
        ) {
            return null;
        }

        return authorizationHeader.substring(
                BEARER_PREFIX.length()
        );
    }
}