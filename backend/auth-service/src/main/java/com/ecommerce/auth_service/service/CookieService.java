package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.config.SecurityProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {

    public static final String REFRESH_TOKEN_COOKIE =
            "refresh_token";

    private final SecurityProperties securityProperties;

    public CookieService(
            SecurityProperties securityProperties
    ) {
        this.securityProperties = securityProperties;
    }

    public ResponseCookie createRefreshTokenCookie(
            String refreshToken
    ) {

        return ResponseCookie.from(
                        REFRESH_TOKEN_COOKIE,
                        refreshToken
                )
                .httpOnly(true)
                .secure(
                        securityProperties
                                .cookie()
                                .secure()
                )
                .sameSite(
                        securityProperties
                                .cookie()
                                .sameSite()
                )
                .path(
                        securityProperties
                                .cookie()
                                .path()
                )
                .maxAge(
                        Duration.ofDays(
                                securityProperties
                                        .refreshToken()
                                        .expirationDays()
                        )
                )
                .build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {

        return ResponseCookie.from(
                        REFRESH_TOKEN_COOKIE,
                        ""
                )
                .httpOnly(true)
                .secure(
                        securityProperties
                                .cookie()
                                .secure()
                )
                .sameSite(
                        securityProperties
                                .cookie()
                                .sameSite()
                )
                .path(
                        securityProperties
                                .cookie()
                                .path()
                )
                .maxAge(Duration.ZERO)
                .build();
    }
}