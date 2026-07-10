package com.healthapp.backend.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class User {
    String id;
    String email;
    String passwordHash;
    Role role;
    Instant createdAt;
    Instant updatedAt;
}
