package com.healthapp.backend.adapter.out.persistence.entity;

import com.healthapp.backend.domain.model.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "appointment_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentLogEntity {

    @Id
    private String id;

    @Column(name = "appointment_id", nullable = false)
    private String appointmentId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private AppointmentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    private AppointmentStatus newStatus;

    private String actor;

    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
