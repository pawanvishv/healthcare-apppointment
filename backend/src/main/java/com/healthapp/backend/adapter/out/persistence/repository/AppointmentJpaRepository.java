package com.healthapp.backend.adapter.out.persistence.repository;

import com.healthapp.backend.adapter.out.persistence.entity.AppointmentEntity;
import com.healthapp.backend.domain.model.AppointmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, String> {

    @Query("SELECT a FROM AppointmentEntity a WHERE a.patientId = :patientId " +
           "AND (:status IS NULL OR a.status = :status) ORDER BY a.createdAt DESC")
    List<AppointmentEntity> findByPatientIdAndStatus(@Param("patientId") String patientId,
                                                      @Param("status") AppointmentStatus status,
                                                      Pageable pageable);

    @Query("SELECT COUNT(a) FROM AppointmentEntity a WHERE a.patientId = :patientId " +
           "AND (:status IS NULL OR a.status = :status)")
    long countByPatientIdAndStatus(@Param("patientId") String patientId,
                                    @Param("status") AppointmentStatus status);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.doctorId = :doctorId " +
           "AND (:status IS NULL OR a.status = :status) ORDER BY a.createdAt DESC")
    List<AppointmentEntity> findByDoctorIdAndStatus(@Param("doctorId") String doctorId,
                                                     @Param("status") AppointmentStatus status,
                                                     Pageable pageable);

    @Query("SELECT COUNT(a) FROM AppointmentEntity a WHERE a.doctorId = :doctorId " +
           "AND (:status IS NULL OR a.status = :status)")
    long countByDoctorIdAndStatus(@Param("doctorId") String doctorId,
                                   @Param("status") AppointmentStatus status);
}
