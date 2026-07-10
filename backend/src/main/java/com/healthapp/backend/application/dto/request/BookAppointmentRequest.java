package com.healthapp.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Book appointment request")
public class BookAppointmentRequest {

    @NotBlank
    private String doctorId;

    @NotBlank
    private String slotId;
}
