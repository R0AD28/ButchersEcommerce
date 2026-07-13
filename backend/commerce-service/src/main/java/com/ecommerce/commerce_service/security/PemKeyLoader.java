package com.ecommerce.commerce_service.security;

import com.ecommerce.commerce_service.config.SecurityProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class PemKeyLoader {

    private final PublicKey publicKey;

    public PemKeyLoader(
            ResourceLoader resourceLoader,
            SecurityProperties securityProperties
    ) {
        this.publicKey = loadPublicKey(
                resourceLoader,
                securityProperties.jwt().publicKeyPath()
        );
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private PublicKey loadPublicKey(
            ResourceLoader resourceLoader,
            String location
    ) {
        try {
            Resource resource = resourceLoader.getResource(location);
            String pem = resource.getContentAsString(StandardCharsets.UTF_8);

            String normalized = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(normalized);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);

            return KeyFactory.getInstance("RSA").generatePublic(keySpec);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "No se pudo cargar la clave pública RSA desde " + location,
                    exception
            );
        }
    }
}
