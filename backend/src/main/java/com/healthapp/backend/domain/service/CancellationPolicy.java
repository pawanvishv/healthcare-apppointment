package com.healthapp.backend.domain.service;

import com.healthapp.backend.domain.exception.IllegalStateTransitionException;
import com.healthapp.backend.domain.model.Appointment;
import com.healthapp.backend.domain.model.AppointmentStatus;

import java.time.Duration;
import java.time.Instant;

public class CancellationPolicy {

    private final long cancellationWindowHours;

    public CancellationPolicy(long cancellationWindowHours) {
        this.cancellationWindowHours = cancellationWindowHours;
    }

    public void validateCancellable(Appointment appointment, Instant slotStartTime, Instant now) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateTransitionException("Appointment is already cancelled");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateTransitionException("Cannot cancel a completed appointment");
        }
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateTransitionException("Only confirmed appointments can be cancelled");
        }

        Duration timeUntilSlot = Duration.between(now, slotStartTime);
        if (timeUntilSlot.toHours() < cancellationWindowHours) {
            throw new IllegalStateTransitionException(
                    "Cancellation must be at least " + cancellationWindowHours + " hours before the appointment");
        }
    }
}
