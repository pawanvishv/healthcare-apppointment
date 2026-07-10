package com.healthapp.backend.application.service;

import com.healthapp.backend.application.dto.request.LoginRequest;
import com.healthapp.backend.application.dto.request.RegisterRequest;
import com.healthapp.backend.application.dto.response.AuthTokenResponse;
import com.healthapp.backend.application.port.out.DoctorRepositoryPort;
import com.healthapp.backend.application.port.out.PatientRepositoryPort;
import com.healthapp.backend.application.port.out.RefreshTokenRepositoryPort;
import com.healthapp.backend.application.port.out.UserRepositoryPort;
import com.healthapp.backend.domain.exception.DomainException;
import com.healthapp.backend.domain.model.Doctor;
import com.healthapp.backend.domain.model.Patient;
import com.healthapp.backend.domain.model.Role;
import com.healthapp.backend.domain.model.User;
import com.healthapp.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepositoryPort userRepository;
    private final DoctorRepositoryPort doctorRepository;
    private final PatientRepositoryPort patientRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DomainException("EMAIL_EXISTS", "Email is already registered");
        }

        if (request.getRole() == Role.DOCTOR && (request.getSpecialization() == null
                || request.getSpecialization().isBlank())) {
            throw new DomainException("VALIDATION_ERROR", "Specialization is required for doctors");
        }

        Instant now = Instant.now();
        String userId = UUID.randomUUID().toString();

        User user = User.builder()
                .id(userId)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(user);

        if (request.getRole() == Role.DOCTOR) {
            doctorRepository.save(Doctor.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .specialization(request.getSpecialization())
                    .createdAt(now)
                    .build());
        } else if (request.getRole() == Role.PATIENT) {
            patientRepository.save(Patient.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .phone(request.getPhone())
                    .createdAt(now)
                    .build());
        }

        return issueTokens(user);
    }

    public AuthTokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        var stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (stored.revoked() || stored.expiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.revokeAllForUser(stored.userId());
            throw new BadCredentialsException("Refresh token reuse detected or expired");
        }

        User user = userRepository.findById(stored.userId())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        refreshTokenRepository.revoke(stored.id());
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> refreshTokenRepository.revoke(token.id()));
    }

    private AuthTokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = generateRefreshToken();
        String tokenHash = hashToken(refreshToken);

        refreshTokenRepository.save(user.getId(), tokenHash,
                Instant.now().plusMillis(refreshTokenExpirationMs));

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .build();
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
