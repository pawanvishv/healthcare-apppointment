package com.healthapp.backend.application.port.out;

import com.healthapp.backend.domain.model.Appointment;
import com.healthapp.backend.domain.model.AppointmentStatus;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepositoryPort {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(String id);

    List<Appointment> findByPatientId(String patientId, AppointmentStatus status, int page, int size);

    long countByPatientId(String patientId, AppointmentStatus status);

    List<Appointment> findByDoctorId(String doctorId, AppointmentStatus status, int page, int size);

    long countByDoctorId(String doctorId, AppointmentStatus status);
}
