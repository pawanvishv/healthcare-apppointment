package com.healthapp.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Authentication token response")
public class AuthTokenResponse {
    String accessToken;
    String refreshToken;
    String tokenType;
    long expiresIn;
}
