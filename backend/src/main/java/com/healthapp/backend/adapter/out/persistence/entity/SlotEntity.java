package com.healthapp.backend.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotEntity {

    @Id
    private String id;

    @Column(name = "doctor_id", nullable = false)
    private String doctorId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(nullable = false)
    private boolean available;

    @Version
    @Column(nullable = false)
    private int version;
}
