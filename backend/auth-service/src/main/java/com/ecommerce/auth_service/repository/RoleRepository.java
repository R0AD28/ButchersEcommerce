package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository
        extends JpaRepository<Role, Long> {

    Optional<Role> findByNameIgnoreCase(String name);
}