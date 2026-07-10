package com.healthapp.backend.adapter.out.persistence.entity;

import com.healthapp.backend.domain.model.ProcessingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationStatusEntity {

    @Id
    private String id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private String appointmentId;

    @Column(nullable = false)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;

    @Column(name = "attempted_at")
    private Instant attemptedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
