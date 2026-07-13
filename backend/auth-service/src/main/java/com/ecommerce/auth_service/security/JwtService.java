package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.config.SecurityProperties;
import com.ecommerce.auth_service.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class JwtService {

    private static final String EXPECTED_ALGORITHM = "RS256";
    private static final String EMAIL_CLAIM = "email";

    private final PemKeyLoader pemKeyLoader;
    private final SecurityProperties securityProperties;

    public JwtService(
            PemKeyLoader pemKeyLoader,
            SecurityProperties securityProperties
    ) {
        this.pemKeyLoader = pemKeyLoader;
        this.securityProperties = securityProperties;
    }

    public String generateAccessToken(User user) {

        Instant issuedAt = Instant.now();

        Instant expiration = issuedAt.plusSeconds(
                getAccessTokenExpirationSeconds()
        );

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(EMAIL_CLAIM, user.getEmail())
                .issuer(securityProperties.jwt().issuer())
                .audience()
                    .add(securityProperties.jwt().audience())
                    .and()
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(
                        pemKeyLoader.getPrivateKey(),
                        Jwts.SIG.RS256
                )
                .compact();
    }

    public Claims parseAndValidateToken(String token) {

        if (token == null || token.isBlank()) {
            throw new JwtException(
                    "El token JWT no puede estar vacío"
            );
        }

        Jws<Claims> signedClaims = Jwts.parser()
                .verifyWith(pemKeyLoader.getPublicKey())
                .build()
                .parseSignedClaims(token);

        String algorithm =
                signedClaims.getHeader().getAlgorithm();

        if (!EXPECTED_ALGORITHM.equals(algorithm)) {
            throw new JwtException(
                    "El algoritmo del JWT no es válido"
            );
        }

        Claims claims = signedClaims.getPayload();

        validateIssuer(claims);
        validateAudience(claims);
        validateRequiredClaims(claims);

        return claims;
    }

    public boolean isTokenValid(String token) {

        try {

            parseAndValidateToken(token);
            return true;

        } catch (
                JwtException |
                IllegalArgumentException exception
        ) {

            return false;
        }
    }

    public Long extractUserId(String token) {

        String subject =
                parseAndValidateToken(token).getSubject();

        try {

            return Long.valueOf(subject);

        } catch (NumberFormatException exception) {

            throw new JwtException(
                    "El subject del JWT no contiene un identificador válido",
                    exception
            );
        }
    }

    public String extractEmail(String token) {

        return parseAndValidateToken(token)
                .get(EMAIL_CLAIM, String.class);
    }

    public String extractTokenId(String token) {

        return parseAndValidateToken(token)
                .getId();
    }

    public long getAccessTokenExpirationSeconds() {

        return securityProperties
                .jwt()
                .accessTokenExpirationSeconds();
    }

    private void validateIssuer(Claims claims) {

        String expectedIssuer =
                securityProperties.jwt().issuer();

        if (!Objects.equals(
                expectedIssuer,
                claims.getIssuer()
        )) {

            throw new JwtException(
                    "El emisor del JWT no es válido"
            );
        }
    }

    private void validateAudience(Claims claims) {

        Object audienceClaim = claims.get("aud");

        String expectedAudience =
                securityProperties.jwt().audience();

        if (!containsAudience(
                audienceClaim,
                expectedAudience
        )) {

            throw new JwtException(
                    "La audiencia del JWT no es válida"
            );
        }
    }

    private boolean containsAudience(
            Object audienceClaim,
            String expectedAudience
    ) {

        if (audienceClaim instanceof String audience) {

            return expectedAudience.equals(audience);
        }

        if (audienceClaim instanceof Collection<?> audiences) {

            return audiences.stream()
                    .map(String::valueOf)
                    .anyMatch(expectedAudience::equals);
        }

        return false;
    }

    private void validateRequiredClaims(Claims claims) {

        if (
                claims.getSubject() == null ||
                claims.getSubject().isBlank()
        ) {

            throw new JwtException(
                    "El JWT no contiene el identificador del usuario"
            );
        }

        String email =
                claims.get(EMAIL_CLAIM, String.class);

        if (email == null || email.isBlank()) {

            throw new JwtException(
                    "El JWT no contiene el correo del usuario"
            );
        }

        if (
                claims.getId() == null ||
                claims.getId().isBlank()
        ) {

            throw new JwtException(
                    "El JWT no contiene un identificador jti"
            );
        }

        if (claims.getIssuedAt() == null) {

            throw new JwtException(
                    "El JWT no contiene la fecha de emisión"
            );
        }

        if (claims.getExpiration() == null) {

            throw new JwtException(
                    "El JWT no contiene la fecha de expiración"
            );
        }
    }
}