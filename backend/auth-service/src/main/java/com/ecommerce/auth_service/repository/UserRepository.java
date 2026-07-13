package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {
            "role",
            "role.permissions"
    })
    @Query("""
            SELECT u
            FROM User u
            WHERE u.id = :userId
            """)
    Optional<User> findWithAuthoritiesById(
            @Param("userId") Long userId
    );
}