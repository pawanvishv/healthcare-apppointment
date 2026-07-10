package com.healthapp.backend.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class Patient {
    String id;
    String userId;
    String phone;
    Instant createdAt;
}
