package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.AuthorizationContext;
import com.ecommerce.auth_service.service.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/authorization")
public class InternalAuthorizationController {

    private final AuthorizationService authorizationService;

    public InternalAuthorizationController(
            AuthorizationService authorizationService
    ) {
        this.authorizationService =
                authorizationService;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthorizationContext> getCurrentAuthorization(
            Authentication authentication
    ) {

        Long userId =
                extractAuthenticatedUserId(
                        authentication
                );

        AuthorizationContext context =
                authorizationService
                        .getAuthorizationContext(userId);

        return ResponseEntity.ok(context);
    }

    private Long extractAuthenticatedUserId(
            Authentication authentication
    ) {

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof Long userId) {
            return userId;
        }

        return Long.valueOf(
                principal.toString()
        );
    }
}