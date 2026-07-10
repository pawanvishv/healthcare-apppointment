package com.healthapp.backend.domain.exception;

public class SlotAlreadyBookedException extends DomainException {

    public SlotAlreadyBookedException() {
        super("SLOT_UNAVAILABLE", "Slot no longer available");
    }
}
