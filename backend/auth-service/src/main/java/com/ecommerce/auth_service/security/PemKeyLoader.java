package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.config.SecurityProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class PemKeyLoader {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public PemKeyLoader(
            SecurityProperties securityProperties,
            ResourceLoader resourceLoader
    ) {

        SecurityProperties.Jwt jwtProperties =
                securityProperties.jwt();

        this.privateKey = loadPrivateKey(
                jwtProperties.privateKeyPath(),
                resourceLoader
        );

        this.publicKey = loadPublicKey(
                jwtProperties.publicKeyPath(),
                resourceLoader
        );
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private PrivateKey loadPrivateKey(
            String location,
            ResourceLoader resourceLoader
    ) {

        try {

            String pemContent = readPemFile(
                    location,
                    resourceLoader
            );

            String normalizedPem = pemContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes =
                    Base64.getDecoder().decode(normalizedPem);

            PKCS8EncodedKeySpec keySpec =
                    new PKCS8EncodedKeySpec(keyBytes);

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "No se pudo cargar la clave privada RSA desde: "
                            + location,
                    exception
            );
        }
    }

    private PublicKey loadPublicKey(
            String location,
            ResourceLoader resourceLoader
    ) {

        try {

            String pemContent = readPemFile(
                    location,
                    resourceLoader
            );

            String normalizedPem = pemContent
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes =
                    Base64.getDecoder().decode(normalizedPem);

            X509EncodedKeySpec keySpec =
                    new X509EncodedKeySpec(keyBytes);

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(keySpec);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "No se pudo cargar la clave pública RSA desde: "
                            + location,
                    exception
            );
        }
    }

    private String readPemFile(
            String location,
            ResourceLoader resourceLoader
    ) throws IOException {

        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException(
                    "La ubicación de la clave PEM no puede estar vacía"
            );
        }

        Resource resource =
                resourceLoader.getResource(location);

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "No existe el archivo PEM: " + location
            );
        }

        try (var inputStream = resource.getInputStream()) {

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }
}