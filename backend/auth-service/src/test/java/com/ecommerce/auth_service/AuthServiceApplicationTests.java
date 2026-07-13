package com.ecommerce.auth_service;

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
class AuthServiceApplicationTests {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("auth_service_test")
                    .withUsername("auth_test")
                    .withPassword("auth_test_password");

    @DynamicPropertySource
    @SuppressWarnings("unused")
    static void configurePostgreSQL(
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
    }

    @Test
    void contextLoads() {
    }
}
