package com.ecommerce.commerce_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CommerceServiceApplicationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("commerce_service_test")
                    .withUsername("catalog_test")
                    .withPassword("catalog_test_password");

    @DynamicPropertySource
    @SuppressWarnings("unused")
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRESQL::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRESQL::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRESQL::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRESQL::getDriverClassName
        );

        registry.add(
                "security.jwt.public-key-path",
                () -> "classpath:test-keys/public-key.pem"
        );

        registry.add(
                "security.jwt.issuer",
                () -> "auth-service-test"
        );

        registry.add(
                "security.jwt.audience",
                () -> "butchers-ecommerce-test"
        );

        registry.add(
                "security.services.auth.url",
                () -> "http://localhost:8081"
        );

        registry.add(
                "security.services.auth.service-token",
                () -> "test-catalog-token"
        );

        registry.add(
                "security.services.audit.url",
                () -> "http://localhost:8085"
        );

        registry.add(
                "security.services.audit.service-token",
                () -> "test-catalog-token"
        );
    }

    @Test
    void contextLoads() {
    }
}