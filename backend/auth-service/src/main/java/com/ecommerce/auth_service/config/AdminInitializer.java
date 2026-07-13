package com.ecommerce.auth_service.config;

import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.repository.RoleRepository;
import com.ecommerce.auth_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {

        String normalizedEmail = adminEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        Role adminRole = roleRepository
                .findByNameIgnoreCase("ADMINISTRADOR")
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No existe el rol ADMINISTRADOR"
                        )
                );

        User administrator = User.builder()
                .email(normalizedEmail)
                .passwordHash(
                        passwordEncoder.encode(adminPassword)
                )
                .enabled(true)
                .failedLoginAttempts(0)
                .role(adminRole)
                .build();

        userRepository.save(administrator);
    }
}