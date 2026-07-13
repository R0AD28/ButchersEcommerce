package com.ecommerce.audit_service;

import org.junit.jupiter.api.Test;

/** Prueba mínima que no requiere claves ni base externa. */
class AuditServiceApplicationTests {

    @Test
    void applicationClassExists() {
        // Esta prueba básica permite que CI compile el proyecto desde el inicio.
        // Las pruebas de integración se agregan cuando el servicio entra al compose.
        new AuditServiceApplication();
    }
}
