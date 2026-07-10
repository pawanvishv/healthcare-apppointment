package com.healthapp.backend.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    private String phone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
