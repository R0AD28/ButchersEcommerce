package com.ecommerce.auth_service.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.dto.UserResponse;
import com.ecommerce.auth_service.exception.BusinessException;
import com.ecommerce.auth_service.exception.ErrorCode;
import com.ecommerce.auth_service.exception.InvalidCredentialsException;
import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.repository.RoleRepository;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.auth_service.security.JwtService;
import com.ecommerce.auth_service.service.model.AuthResult;
import com.ecommerce.auth_service.service.model.RefreshResult;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final LoginAttemptService loginAttemptService;

    private final AuthorizationService authorizationService;

    private final AuditClient auditClient;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            LoginAttemptService loginAttemptService,
            AuthorizationService authorizationService,
            AuditClient auditClient
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService
                = refreshTokenService;
        this.loginAttemptService
                = loginAttemptService;
        this.authorizationService
                = authorizationService;
        this.auditClient = auditClient;
    }

    @Transactional
    public UserResponse register(
            RegisterRequest request,
            HttpServletRequest httpRequest
    ) {

        String normalizedEmail
                = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(
                normalizedEmail
        )) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    "El correo ya se encuentra registrado"
            );
        }

        Role clientRole = roleRepository
                .findByNameIgnoreCase("CLIENTE")
                .orElseThrow(
                        () -> new BusinessException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                ErrorCode.ROLE_NOT_FOUND,
                                "No se encontró el rol CLIENTE"
                        )
                );

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .enabled(true)
                .failedLoginAttempts(0)
                .role(clientRole)
                .build();

        userRepository.save(user);

        auditClient.send(
                user.getId(),
                user.getEmail(),
                "USER_REGISTERED",
                "USER",
                user.getId().toString(),
                "SUCCESS",
                resolveClientIp(httpRequest),
                null
        );

        return UserResponse.from(user);
    }

    @Transactional
    public AuthResult login(
            String rawEmail,
            String rawPassword,
            HttpServletRequest httpRequest
    ) {

        String email = normalizeEmail(rawEmail);

        String ipAddress
                = resolveClientIp(httpRequest);

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElse(null);

        /*
         * Se usa una respuesta genérica para no revelar
         * si el correo se encuentra registrado.
         */
        if (user == null) {

            auditClient.send(
                    null,
                    email,
                    "LOGIN_FAILED",
                    "USER",
                    null,
                    "FAILURE",
                    ipAddress,
                    "Credenciales inválidas"
            );

            throw invalidCredentials();
        }

        if (!user.isEnabled()) {

            auditClient.send(
                    user.getId(),
                    user.getEmail(),
                    "LOGIN_FAILED",
                    "USER",
                    user.getId().toString(),
                    "FAILURE",
                    ipAddress,
                    "Cuenta deshabilitada"
            );

            throw invalidCredentials();
        }

        loginAttemptService.verifyNotLocked(user);

        boolean passwordMatches
                = passwordEncoder.matches(
                        rawPassword,
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            loginAttemptService.registerFailure(user);
            authorizationService.evict(user.getId());

            auditClient.send(
                    user.getId(),
                    user.getEmail(),
                    "LOGIN_FAILED",
                    "USER",
                    user.getId().toString(),
                    "FAILURE",
                    ipAddress,
                    "Credenciales inválidas"
            );

            throw invalidCredentials();
        }

        loginAttemptService.registerSuccess(user);

        authorizationService.evict(user.getId());

        String accessToken
                = jwtService.generateAccessToken(user);

        String refreshToken
                = refreshTokenService.create(
                        user,
                        ipAddress
                );

        auditClient.send(
                user.getId(),
                user.getEmail(),
                "LOGIN_SUCCESS",
                "USER",
                user.getId().toString(),
                "SUCCESS",
                ipAddress,
                null
        );

        return new AuthResult(
                accessToken,
                refreshToken,
                jwtService
                        .getAccessTokenExpirationSeconds()
        );
    }

    @Transactional
    public RefreshResult refresh(
            String rawRefreshToken,
            HttpServletRequest httpRequest
    ) {

        String ipAddress
                = resolveClientIp(httpRequest);

        RefreshTokenService.RotationResult rotation
                = refreshTokenService.rotate(
                        rawRefreshToken,
                        ipAddress
                );

        User user = rotation.user();

        if (!user.isEnabled()
                || user.isTemporarilyLocked()) {

            refreshTokenService.revokeAll(
                    user.getId()
            );

            authorizationService.evict(
                    user.getId()
            );

            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCode.ACCOUNT_DISABLED,
                    "La cuenta no está disponible"
            );
        }

        String accessToken
                = jwtService.generateAccessToken(user);

        auditClient.send(
                user.getId(),
                user.getEmail(),
                "TOKEN_REFRESHED",
                "SESSION",
                null,
                "SUCCESS",
                ipAddress,
                null
        );

        return new RefreshResult(
                accessToken,
                rotation.newRefreshToken(),
                jwtService
                        .getAccessTokenExpirationSeconds()
        );
    }

    @Transactional
    public void logout(
            String rawRefreshToken,
            HttpServletRequest httpRequest
    ) {

        refreshTokenService.revoke(
                rawRefreshToken
        );

        auditClient.send(
                null,
                null,
                "SESSION_REVOKED",
                "SESSION",
                null,
                "SUCCESS",
                resolveClientIp(httpRequest),
                null
        );
    }

    private InvalidCredentialsException invalidCredentials() {

        return new InvalidCredentialsException(
                "Credenciales inválidas o cuenta temporalmente bloqueada"
        );
    }

    private String normalizeEmail(String email) {

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String resolveClientIp(
            HttpServletRequest request
    ) {

        String forwardedFor
                = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null
                && !forwardedFor.isBlank()) {

            return forwardedFor
                    .split(",")[0]
                    .trim();
        }

        return request.getRemoteAddr();
    }
}
