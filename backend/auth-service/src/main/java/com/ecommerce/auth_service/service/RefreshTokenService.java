package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.config.SecurityProperties;
import com.ecommerce.auth_service.exception.BusinessException;
import com.ecommerce.auth_service.exception.ErrorCode;
import com.ecommerce.auth_service.model.RefreshToken;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final int TOKEN_SIZE_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;

    private final SecurityProperties securityProperties;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            SecurityProperties securityProperties
    ) {
        this.refreshTokenRepository =
                refreshTokenRepository;

        this.securityProperties =
                securityProperties;
    }

    @Transactional
    public String create(
            User user,
            String ipAddress
    ) {

        String rawToken = generateToken();

        RefreshToken refreshToken =
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(hash(rawToken))
                        .expiresAt(
                                Instant.now().plus(
                                        securityProperties
                                                .refreshToken()
                                                .expirationDays(),
                                        ChronoUnit.DAYS
                                )
                        )
                        .createdIp(ipAddress)
                        .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RotationResult rotate(
            String rawToken,
            String ipAddress
    ) {

        if (rawToken == null || rawToken.isBlank()) {
            throw invalidRefreshToken();
        }

        RefreshToken currentToken =
                refreshTokenRepository
                        .findByTokenHash(hash(rawToken))
                        .orElseThrow(
                                this::invalidRefreshToken
                        );

        /*
         * Si el token ya fue revocado o reemplazado,
         * puede tratarse de reutilización.
         */
        if (!currentToken.isActive()) {

            refreshTokenRepository
                    .revokeAllActiveByUserId(
                            currentToken
                                    .getUser()
                                    .getId(),
                            Instant.now()
                    );

            throw invalidRefreshToken();
        }

        String newRawToken = generateToken();
        String newTokenHash = hash(newRawToken);

        currentToken.setRevokedAt(Instant.now());
        currentToken.setReplacedByTokenHash(
                newTokenHash
        );

        refreshTokenRepository.save(currentToken);

        RefreshToken replacement =
                RefreshToken.builder()
                        .user(currentToken.getUser())
                        .tokenHash(newTokenHash)
                        .expiresAt(
                                Instant.now().plus(
                                        securityProperties
                                                .refreshToken()
                                                .expirationDays(),
                                        ChronoUnit.DAYS
                                )
                        )
                        .createdIp(ipAddress)
                        .build();

        refreshTokenRepository.save(replacement);

        return new RotationResult(
                currentToken.getUser(),
                newRawToken
        );
    }

    @Transactional
    public void revoke(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository
                .findByTokenHash(hash(rawToken))
                .ifPresent(refreshToken -> {

                    if (refreshToken.getRevokedAt() == null) {

                        refreshToken.setRevokedAt(
                                Instant.now()
                        );

                        refreshTokenRepository.save(
                                refreshToken
                        );
                    }
                });
    }

    @Transactional
    public void revokeAll(Long userId) {

        refreshTokenRepository
                .revokeAllActiveByUserId(
                        userId,
                        Instant.now()
                );
    }

    private String generateToken() {

        byte[] randomBytes =
                new byte[TOKEN_SIZE_BYTES];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hash(String rawToken) {

        try {

            MessageDigest messageDigest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] digest = messageDigest.digest(
                    rawToken.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return HexFormat.of()
                    .formatHex(digest);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 no está disponible",
                    exception
            );
        }
    }

    private BusinessException invalidRefreshToken() {

        return new BusinessException(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_REFRESH_TOKEN,
                "El refresh token no es válido"
        );
    }

    public record RotationResult(
            User user,
            String newRefreshToken
    ) {
    }
}