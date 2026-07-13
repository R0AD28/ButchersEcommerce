package com.ecommerce.audit_service.filter;

import com.ecommerce.audit_service.dto.AuthorizationContext;
import com.ecommerce.audit_service.security.JwtService;
import com.ecommerce.audit_service.service.AuthorizationClient;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Autentica las rutas administrativas.
 *
 * 1. Valida localmente la firma RS256 del JWT.
 * 2. Consulta a auth-service los privilegios actuales.
 * 3. Construye Authentication con ROLE_* y permisos.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Las escrituras internas se protegen con credenciales de servicio,
        // ya que algunos eventos (LOGIN_FAILED) no disponen de JWT de usuario.
        return request.getRequestURI().startsWith("/internal/")
                || request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorizationHeader.substring(7);
            Claims claims = jwtService.parseAndValidate(token);
            AuthorizationContext context = authorizationClient
                    .getCurrentAuthorization(authorizationHeader);

            if (context == null || !context.enabled() || context.locked()) {
                filterChain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            context.roles().forEach(role -> authorities.add(
                    new SimpleGrantedAuthority("ROLE_" + role)
            ));
            context.permissions().forEach(permission -> authorities.add(
                    new SimpleGrantedAuthority(permission)
            ));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            claims.getSubject(),
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ignored) {
            // No se revela al cliente si falló la firma, expiración, audiencia
            // o la consulta interna. Spring responderá 401 de forma uniforme.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
