package com.healthapp.backend.application.port.out;

import com.healthapp.backend.domain.model.AppointmentLog;

import java.util.List;

public interface AppointmentLogRepositoryPort {

    AppointmentLog save(AppointmentLog log);

    List<AppointmentLog> findByAppointmentId(String appointmentId);
}
