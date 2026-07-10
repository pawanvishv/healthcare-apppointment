package com.healthapp.backend.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class NotificationStatusRecord {
    String id;
    String appointmentId;
    String channel;
    ProcessingStatus status;
    Instant attemptedAt;
    Instant processedAt;
}
