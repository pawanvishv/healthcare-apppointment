package com.healthapp.backend.domain.service;

import com.healthapp.backend.domain.model.Slot;

import java.time.Instant;

public class SlotAvailabilityPolicy {

    public void validateBookable(Slot slot, Instant now) {
        if (!slot.isAvailable()) {
            throw new com.healthapp.backend.domain.exception.SlotAlreadyBookedException();
        }
        if (!slot.getStartTime().isAfter(now)) {
            throw new com.healthapp.backend.domain.exception.DomainException(
                    "SLOT_IN_PAST", "Cannot book a slot in the past");
        }
    }

    public void validateBelongsToDoctor(Slot slot, String doctorId) {
        if (!slot.getDoctorId().equals(doctorId)) {
            throw new com.healthapp.backend.domain.exception.DomainException(
                    "SLOT_DOCTOR_MISMATCH", "Slot does not belong to the requested doctor");
        }
    }
}
