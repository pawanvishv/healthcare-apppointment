package com.healthapp.backend.application.port.out;

import com.healthapp.backend.domain.model.Slot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SlotRepositoryPort {

    Optional<Slot> findById(String id);

    List<Slot> findAvailableByDoctorAndDate(String doctorId, LocalDate date);

    Slot reserveSlot(String slotId, int expectedVersion);

    void releaseSlot(String slotId);
}
