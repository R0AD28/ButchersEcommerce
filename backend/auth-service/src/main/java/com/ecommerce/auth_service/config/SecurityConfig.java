package com.ecommerce.auth_service.config;

import com.ecommerce.auth_service.security.JwtAuthenticationFilter;
import com.ecommerce.auth_service.security.RateLimitFilter;
import com.ecommerce.auth_service.security.RestAccessDeniedHandler;
import com.ecommerce.auth_service.security.RestAuthenticationEntryPoint;
import com.ecommerce.auth_service.security.ServiceTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ServiceTokenFilter serviceTokenFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ServiceTokenFilter serviceTokenFilter,
            RateLimitFilter rateLimitFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            @Value(
                    "${security.cors.allowed-origins:"
                            + "http://localhost:3000}"
            )
            String allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.serviceTokenFilter = serviceTokenFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http

                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .headers(headers ->
                        headers
                                /*
                                 * Evita que el navegador intente
                                 * adivinar tipos de contenido.
                                 */
                                .contentTypeOptions(Customizer.withDefaults())

                                /*
                                 * Impide cargar la aplicación
                                 * dentro de frames.
                                 */
                                .frameOptions(frame -> frame.deny())

                                /*
                                 * Auth-service es una API JSON,
                                 * por eso no necesita cargar recursos.
                                 */
                                .contentSecurityPolicy(csp ->
                                        csp.policyDirectives(
                                                "default-src 'none'; "
                                                        + "frame-ancestors "
                                                        + "'none'; "
                                                        + "base-uri 'none'; "
                                                        + "form-action 'none'"
                                        )
                                )

                                .referrerPolicy(referrer ->
                                        referrer.policy(
                                                ReferrerPolicyHeaderWriter
                                                        .ReferrerPolicy
                                                        .NO_REFERRER
                                        )
                                )

                                /*
                                 * Spring solo envía HSTS cuando
                                 * la solicitud utiliza HTTPS.
                                 */
                                .httpStrictTransportSecurity(hsts ->
                                        hsts
                                                .includeSubDomains(true)
                                                .preload(true)
                                                .maxAgeInSeconds(
                                                        31_536_000
                                                )
                                )
                )

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                )

                .authorizeHttpRequests(auth ->
                        auth

                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/auth/register",
                                        "/auth/login",
                                        "/auth/refresh",
                                        "/auth/logout",
                                        "/actuator/health"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/internal/**"
                                )
                                .authenticated()

                                .requestMatchers(
                                        "/admin/**"
                                )
                                .authenticated()

                                .anyRequest()
                                .authenticated()
                )

                /*
                 * Rate limiting se ejecuta antes de  para evitar
                 * procesar solicitudes excesivas.
                 */
                .addFilterBefore(
                        rateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                /*
                * Después se valida el JWT.
                */
                .addFilterAfter(
                        jwtAuthenticationFilter,
                        RateLimitFilter.class
                )

                /*
                * Para /internal/** se valida además el token
                * del microservicio.
                */
                .addFilterAfter(
                        serviceTokenFilter,
                        JwtAuthenticationFilter.class
                )

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        List<String> origins =
                Arrays.stream(
                                allowedOrigins.split(",")
                        )
                        .map(String::trim)
                        .filter(origin ->
                                !origin.isBlank()
                        )
                        .toList();

        configuration.setAllowedOrigins(
                origins
        );

        configuration.setAllowedMethods(
                List.of(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name()
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        HttpHeaders.AUTHORIZATION,
                        HttpHeaders.CONTENT_TYPE,
                        HttpHeaders.ACCEPT,
                        "X-Service-Name",
                        "X-Service-Token",
                        "X-Correlation-Id"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "X-Correlation-Id",
                        "X-RateLimit-Limit",
                        "X-RateLimit-Remaining",
                        "X-RateLimit-Reset",
                        "Retry-After"
                )
        );

        configuration.setAllowCredentials(
                true
        );

        configuration.setMaxAge(
                3600L
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
    disableJwtFilterServletRegistration(
            JwtAuthenticationFilter filter
    ) {

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }

    @Bean
    public FilterRegistrationBean<ServiceTokenFilter>
    disableServiceTokenFilterServletRegistration(
            ServiceTokenFilter filter
    ) {

        FilterRegistrationBean<ServiceTokenFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter>
    disableRateLimitFilterServletRegistration(
            RateLimitFilter filter
    ) {

        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }
}