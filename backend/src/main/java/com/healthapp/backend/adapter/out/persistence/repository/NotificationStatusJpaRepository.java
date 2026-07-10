package com.healthapp.backend.adapter.out.persistence.repository;

import com.healthapp.backend.adapter.out.persistence.entity.NotificationStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationStatusJpaRepository extends JpaRepository<NotificationStatusEntity, String> {

    Optional<NotificationStatusEntity> findByAppointmentId(String appointmentId);
}
