package com.ecommerce.audit_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Carga únicamente la clave pública RSA.
 * audit-service nunca debe tener acceso a la clave privada de auth-service.
 */
@Component
public class PemKeyLoader {

    private final PublicKey publicKey;

    public PemKeyLoader(
            ResourceLoader resourceLoader,
            @Value("${security.jwt.public-key-path}") String publicKeyPath
    ) {
        this.publicKey = loadPublicKey(resourceLoader.getResource(publicKeyPath));
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private PublicKey loadPublicKey(Resource resource) {
        try {
            String pem = resource.getContentAsString(StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\s", "");

            byte[] decoded = Base64.getDecoder().decode(pem);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "No se pudo cargar la clave pública RSA desde " + resource,
                    exception
            );
        }
    }
}
