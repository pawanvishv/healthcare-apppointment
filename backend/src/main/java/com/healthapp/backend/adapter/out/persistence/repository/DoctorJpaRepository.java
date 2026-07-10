package com.healthapp.backend.adapter.out.persistence.repository;

import com.healthapp.backend.adapter.out.persistence.entity.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorJpaRepository extends JpaRepository<DoctorEntity, String> {

    Optional<DoctorEntity> findByUserId(String userId);
}
