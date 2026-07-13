package com.ecommerce.commerce_service.dto;

import java.util.Set;

public record AuthorizationContext(
        Long userId,
        String email,
        boolean enabled,
        boolean locked,
        Set<String> roles,
        Set<String> permissions
) {
}
