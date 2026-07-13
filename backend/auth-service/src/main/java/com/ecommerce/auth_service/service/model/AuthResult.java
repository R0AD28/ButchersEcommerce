package com.ecommerce.auth_service.service.model;

public record AuthResult(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}