package com.ecommerce.auth_service.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
