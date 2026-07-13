package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.AssignRoleRequest;
import com.ecommerce.auth_service.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RoleService roleService;

    public AdminController(
            RoleService roleService
    ) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<List<String>> listRoles() {

        return ResponseEntity.ok(
                roleService.listRoleNames()
        );
    }

    @PutMapping("/role-assignment")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<Void> assignRole(
            @Valid @RequestBody AssignRoleRequest request,
            Authentication authentication
    ) {

        Long administratorId =
                (Long) authentication.getPrincipal();

        String administratorEmail =
                (String) authentication.getDetails();

        roleService.replaceRole(
                request.email(),
                request.role(),
                administratorId,
                administratorEmail
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}