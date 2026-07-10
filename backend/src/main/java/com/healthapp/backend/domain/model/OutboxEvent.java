package com.healthapp.backend.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class OutboxEvent {
    String id;
    String aggregateId;
    String eventType;
    String payload;
    OutboxEventStatus status;
    Instant createdAt;
    Instant sentAt;
}
