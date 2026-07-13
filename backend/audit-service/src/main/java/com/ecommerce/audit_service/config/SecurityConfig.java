package com.ecommerce.audit_service.config;

import com.ecommerce.audit_service.filter.CorrelationIdFilter;
import com.ecommerce.audit_service.filter.JwtAuthenticationFilter;
import com.ecommerce.audit_service.filter.RateLimitFilter;
import com.ecommerce.audit_service.security.RestAccessDeniedHandler;
import com.ecommerce.audit_service.security.RestAuthenticationEntryPoint;
import com.ecommerce.audit_service.filter.ServiceTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Configuración de seguridad stateless del audit-service. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ServiceTokenFilter serviceTokenFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            ServiceTokenFilter serviceTokenFilter,
            CorrelationIdFilter correlationIdFilter,
            RateLimitFilter rateLimitFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtFilter = jwtFilter;
        this.serviceTokenFilter = serviceTokenFilter;
        this.correlationIdFilter = correlationIdFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // API stateless: no usa formularios, sesiones ni cookies de autenticación.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Headers defensivos para una API JSON.
                .headers(headers -> headers
                        .contentTypeOptions(options -> { })
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
                        ))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"
                        )))
                // Orden: correlación -> credencial interna -> JWT de usuario.
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(serviceTokenFilter, CorrelationIdFilter.class)
                .addFilterAfter(jwtFilter, ServiceTokenFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
                .build();
    }
}
