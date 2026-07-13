package com.ecommerce.auth_service.dto;

import java.io.Serializable;
import java.util.Set;

public record AuthorizationContext(
        Long userId,
        String email,
        boolean enabled,
        boolean locked,
        Set<String> roles,
        Set<String> permissions
) implements Serializable {}
