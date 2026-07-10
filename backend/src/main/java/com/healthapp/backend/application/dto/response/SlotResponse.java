package com.healthapp.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@Schema(description = "Available slot response")
public class SlotResponse {
    String id;
    String doctorId;
    Instant startTime;
    Instant endTime;
    boolean available;
}
