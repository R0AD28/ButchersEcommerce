package com.ecommerce.auth_service.service.model;

public record RefreshResult(
        String accessToken,
        String newRefreshToken,
        long expiresIn
) {
}