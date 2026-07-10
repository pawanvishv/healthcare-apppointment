package com.healthapp.backend.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AppointmentLog {
    String id;
    String appointmentId;
    String eventType;
    AppointmentStatus oldStatus;
    AppointmentStatus newStatus;
    String actor;
    String message;
    Instant createdAt;
}
