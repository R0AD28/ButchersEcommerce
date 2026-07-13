package com.ecommerce.commerce_service.security;

import com.ecommerce.commerce_service.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;

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

    public Claims parseAndValidateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("El token JWT no puede estar vacío");
        }

        Jws<Claims> signedClaims = Jwts.parser()
                .verifyWith(pemKeyLoader.getPublicKey())
                .build()
                .parseSignedClaims(token);

        if (!EXPECTED_ALGORITHM.equals(signedClaims.getHeader().getAlgorithm())) {
            throw new JwtException("El algoritmo del JWT no es válido");
        }

        Claims claims = signedClaims.getPayload();
        validateIssuer(claims);
        validateAudience(claims);
        validateRequiredClaims(claims);

        return claims;
    }

    public Long extractUserId(String token) {
        try {
            return Long.valueOf(parseAndValidateToken(token).getSubject());
        } catch (NumberFormatException exception) {
            throw new JwtException("El subject del JWT no es válido", exception);
        }
    }

    public String extractEmail(String token) {
        return parseAndValidateToken(token).get(EMAIL_CLAIM, String.class);
    }

    private void validateIssuer(Claims claims) {
        if (!Objects.equals(
                securityProperties.jwt().issuer(),
                claims.getIssuer()
        )) {
            throw new JwtException("El emisor del JWT no es válido");
        }
    }

    private void validateAudience(Claims claims) {
        Object audienceClaim = claims.get("aud");
        String expected = securityProperties.jwt().audience();

        boolean valid = audienceClaim instanceof String audience
                ? expected.equals(audience)
                : audienceClaim instanceof Collection<?> audiences
                && audiences.stream()
                .map(String::valueOf)
                .anyMatch(expected::equals);

        if (!valid) {
            throw new JwtException("La audiencia del JWT no es válida");
        }
    }

    private void validateRequiredClaims(Claims claims) {
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new JwtException("El JWT no contiene el identificador del usuario");
        }

        String email = claims.get(EMAIL_CLAIM, String.class);
        if (email == null || email.isBlank()) {
            throw new JwtException("El JWT no contiene el correo del usuario");
        }

        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new JwtException("El JWT no contiene jti");
        }

        if (claims.getIssuedAt() == null || claims.getExpiration() == null) {
            throw new JwtException("El JWT no contiene fechas válidas");
        }
    }
}
