package com.ecommerce.audit_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PublicKey;

/** Valida firma, emisor, audiencia y expiración del access token. */
@Service
public class JwtService {

    private final PublicKey publicKey;
    private final String issuer;
    private final String audience;

    public JwtService(
            PemKeyLoader keyLoader,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience
    ) {
        this.publicKey = keyLoader.getPublicKey();
        this.issuer = issuer;
        this.audience = audience;
    }

    public Claims parseAndValidate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!claims.getAudience().contains(audience)) {
            throw new IllegalArgumentException("JWT con audiencia inválida");
        }

        return claims;
    }
}
