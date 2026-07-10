package com.healthapp.backend.adapter.in.web.controller;

import com.healthapp.backend.application.dto.ApiResponse;
import com.healthapp.backend.application.dto.request.LoginRequest;
import com.healthapp.backend.application.dto.request.RefreshTokenRequest;
import com.healthapp.backend.application.dto.request.RegisterRequest;
import com.healthapp.backend.application.dto.response.AuthTokenResponse;
import com.healthapp.backend.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokenResponse tokens = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(tokens));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive tokens")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.getRefreshToken())));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
