package com.healthapp.backend.application.port.out;

import com.healthapp.backend.domain.model.NotificationStatusRecord;

import java.util.Optional;

public interface NotificationStatusRepositoryPort {

    NotificationStatusRecord save(NotificationStatusRecord record);

    Optional<NotificationStatusRecord> findByAppointmentId(String appointmentId);
}
