package com.healthapp.backend.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class Appointment {
    String id;
    String patientId;
    String doctorId;
    String slotId;
    AppointmentStatus status;
    Instant createdAt;
    Instant updatedAt;
}
