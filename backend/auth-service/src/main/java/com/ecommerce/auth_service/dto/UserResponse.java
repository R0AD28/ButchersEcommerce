package com.ecommerce.auth_service.dto;

import com.ecommerce.auth_service.model.User;

public record UserResponse(Long id, String email, boolean enabled, String role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.isEnabled(), user.getRole().getName());
    }
}
