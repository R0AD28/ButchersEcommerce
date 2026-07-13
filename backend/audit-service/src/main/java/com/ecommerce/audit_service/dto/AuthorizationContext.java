package com.ecommerce.audit_service.dto;

import java.util.Set;

/**
 * Contexto devuelto por auth-service con el estado actual del usuario.
 * Roles y permisos no se leen del JWT porque pueden cambiar antes de que
 * expire el token.
 */
public record AuthorizationContext(
        String userId,
        String email,
        boolean enabled,
        boolean locked,
        Set<String> roles,
        Set<String> permissions
) {
}
