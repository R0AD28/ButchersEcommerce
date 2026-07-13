package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.exception.BusinessException;
import com.ecommerce.auth_service.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.repository.RoleRepository;
import com.ecommerce.auth_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final AuditClient auditClient;

    public RoleService(
            RoleRepository roleRepository,
            UserRepository userRepository,
            AuthorizationService authorizationService,
            AuditClient auditClient
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.auditClient = auditClient;
    }

    @Transactional(readOnly = true)
    public List<String> listRoleNames() {

        return roleRepository.findAll()
                .stream()
                .map(Role::getName)
                .sorted()
                .toList();
    }

    @Transactional
    public void replaceRole(
            String userEmail,
            String roleName,
            Long administratorId,
            String administratorEmail
    ) {

        String normalizedEmail =
                userEmail.trim().toLowerCase();

        String normalizedRole =
                roleName.trim().toUpperCase();

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new BusinessException(
                                HttpStatus.NOT_FOUND,
                                ErrorCode.USER_NOT_FOUND,
                                "No existe un usuario con ese correo"
                        )
                );

        Role newRole = roleRepository
                .findByNameIgnoreCase(normalizedRole)
                .orElseThrow(() ->
                        new BusinessException(
                                HttpStatus.NOT_FOUND,
                                ErrorCode.ROLE_NOT_FOUND,
                                "El rol solicitado no existe"
                        )
                );

        String previousRole =
                user.getRole() != null
                        ? user.getRole().getName()
                        : null;

        user.setRole(newRole);

        userRepository.saveAndFlush(user);

        authorizationService.evict(
                user.getId()
        );

        auditClient.send(
        administratorId,
        administratorEmail,
        "ROLE_REPLACED",
        "USER",
        user.getId().toString(),
        "SUCCESS",
        null,
        "Usuario: " + user.getEmail()
                + ", rol anterior: " + previousRole
                + ", rol nuevo: " + newRole.getName()
);
    }
}