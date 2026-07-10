package com.healthapp.backend.adapter.out.persistence.repository;

import com.healthapp.backend.adapter.out.persistence.entity.SlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SlotJpaRepository extends JpaRepository<SlotEntity, String> {

    @Query("SELECT s FROM SlotEntity s WHERE s.doctorId = :doctorId AND s.available = true " +
           "AND s.startTime >= :startOfDay AND s.startTime < :endOfDay ORDER BY s.startTime")
    List<SlotEntity> findAvailableByDoctorAndDateRange(@Param("doctorId") String doctorId,
                                                        @Param("startOfDay") Instant startOfDay,
                                                        @Param("endOfDay") Instant endOfDay);
}
