package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.config.SecurityProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Component
public class ServiceTokenValidator {

    private final Map<String, String> serviceTokens;

    public ServiceTokenValidator(
            SecurityProperties securityProperties
    ) {
        this.serviceTokens =
                securityProperties
                        .internalServices()
                        .tokens();
    }

    public boolean isValid(
            String serviceName,
            String receivedToken
    ) {

        if (
                serviceName == null ||
                serviceName.isBlank() ||
                receivedToken == null ||
                receivedToken.isBlank()
        ) {
            return false;
        }

        String expectedToken =
                serviceTokens.get(serviceName);

        if (
                expectedToken == null ||
                expectedToken.isBlank()
        ) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(
                        StandardCharsets.UTF_8
                ),
                receivedToken.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
}