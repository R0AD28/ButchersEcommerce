package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.*;
import com.ecommerce.auth_service.service.AuthService;
import com.ecommerce.auth_service.service.CookieService;
import com.ecommerce.auth_service.service.model.AuthResult;
import com.ecommerce.auth_service.service.model.RefreshResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final CookieService cookieService;

    public AuthController(AuthService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, httpRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        AuthResult result = authService.login(request.email(), request.password(), httpRequest);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE,
                cookieService.createRefreshTokenCookie(result.refreshToken()).toString());
        return ResponseEntity.ok(new LoginResponse(result.accessToken(), "Bearer", result.expiresIn()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request, HttpServletResponse response) {
        RefreshResult result = authService.refresh(refreshToken, request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieService.createRefreshTokenCookie(result.newRefreshToken()).toString());
        return ResponseEntity.ok(new LoginResponse(result.accessToken(), "Bearer", result.expiresIn()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request, HttpServletResponse response) {
        authService.logout(refreshToken, request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.deleteRefreshTokenCookie().toString());
        return ResponseEntity.noContent().build();
    }
}
