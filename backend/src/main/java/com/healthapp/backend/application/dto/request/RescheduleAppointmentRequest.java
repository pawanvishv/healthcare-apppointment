package com.healthapp.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Reschedule appointment request")
public class RescheduleAppointmentRequest {

    @NotBlank
    private String newSlotId;
}
