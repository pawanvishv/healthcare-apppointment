package com.healthapp.backend.adapter.out.persistence.repository;

import com.healthapp.backend.adapter.out.persistence.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientJpaRepository extends JpaRepository<PatientEntity, String> {

    Optional<PatientEntity> findByUserId(String userId);
}
