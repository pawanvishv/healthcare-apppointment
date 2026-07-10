package com.healthapp.backend.adapter.out.persistence.repository;

import com.healthapp.backend.adapter.out.persistence.entity.AppointmentLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentLogJpaRepository extends JpaRepository<AppointmentLogEntity, String> {

    List<AppointmentLogEntity> findByAppointmentIdOrderByCreatedAtAsc(String appointmentId);
}
