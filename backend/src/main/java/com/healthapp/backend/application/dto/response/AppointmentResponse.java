package com.healthapp.backend.application.dto.response;

import com.healthapp.backend.domain.model.AppointmentStatus;
import com.healthapp.backend.domain.model.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Value
@Builder
@Jacksonized
@Schema(description = "Appointment response")
public class AppointmentResponse {
    String id;
    AppointmentStatus status;
    ProcessingStatus processingStatus;
    String doctorId;
    String slotId;
    String patientId;
    Instant startTime;
    Instant endTime;
    Instant createdAt;
}
