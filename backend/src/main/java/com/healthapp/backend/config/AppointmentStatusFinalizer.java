package com.healthapp.backend.config;

import com.healthapp.backend.adapter.out.persistence.entity.AppointmentEntity;
import com.healthapp.backend.adapter.out.persistence.repository.AppointmentJpaRepository;
import com.healthapp.backend.domain.model.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentStatusFinalizer {

    private final AppointmentJpaRepository appointmentRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void finalizeCompletedAppointments() {
        List<AppointmentEntity> confirmed = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .toList();

        Instant now = Instant.now();
        int completed = 0;

        for (AppointmentEntity appointment : confirmed) {
            // Mark as completed if slot time has passed (simplified check via updated_at)
            if (appointment.getUpdatedAt().isBefore(now.minusSeconds(3600))) {
                appointment.setStatus(AppointmentStatus.COMPLETED);
                appointment.setUpdatedAt(now);
                appointmentRepository.save(appointment);
                completed++;
            }
        }

        if (completed > 0) {
            log.info("Finalized {} appointments as COMPLETED", completed);
        }
    }
}
