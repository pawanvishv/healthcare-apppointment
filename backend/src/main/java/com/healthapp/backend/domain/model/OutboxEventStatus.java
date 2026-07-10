package com.healthapp.backend.domain.model;

public enum OutboxEventStatus {
    PENDING,
    SENT,
    FAILED
}
