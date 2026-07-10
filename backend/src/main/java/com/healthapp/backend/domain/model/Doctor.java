package com.healthapp.backend.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class Doctor {
    String id;
    String userId;
    String specialization;
    Instant createdAt;
}
