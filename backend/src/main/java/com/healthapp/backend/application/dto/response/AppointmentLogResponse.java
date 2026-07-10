package com.healthapp.backend.application.dto.response;

import com.healthapp.backend.domain.model.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@Schema(description = "Appointment audit log entry")
public class AppointmentLogResponse {
    String id;
    String eventType;
    AppointmentStatus oldStatus;
    AppointmentStatus newStatus;
    String actor;
    String message;
    Instant createdAt;
}
