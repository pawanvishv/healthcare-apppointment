package com.healthapp.backend.application.port.out;

import com.healthapp.backend.domain.model.Doctor;

import java.util.Optional;

public interface DoctorRepositoryPort {

    Doctor save(Doctor doctor);

    Optional<Doctor> findById(String id);

    Optional<Doctor> findByUserId(String userId);

    boolean existsById(String id);
}
