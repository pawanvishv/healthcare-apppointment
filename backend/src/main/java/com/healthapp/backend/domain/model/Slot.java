package com.healthapp.backend.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class Slot {
    String id;
    String doctorId;
    Instant startTime;
    Instant endTime;
    boolean available;
    int version;
}
