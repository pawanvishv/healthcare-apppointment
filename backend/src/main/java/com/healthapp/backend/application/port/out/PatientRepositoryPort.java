package com.healthapp.backend.application.port.out;

import com.healthapp.backend.domain.model.Patient;

import java.util.Optional;

public interface PatientRepositoryPort {

    Patient save(Patient patient);

    Optional<Patient> findById(String id);

    Optional<Patient> findByUserId(String userId);
}
