package com.healthapp.backend.adapter.out.persistence;

import com.healthapp.backend.adapter.out.persistence.entity.*;
import com.healthapp.backend.adapter.out.persistence.repository.*;
import com.healthapp.backend.application.port.out.*;
import com.healthapp.backend.domain.exception.SlotAlreadyBookedException;
import com.healthapp.backend.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    @Override
    public User save(User user) {
        return toDomain(repository.save(toEntity(user)));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    private UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

@Component
@RequiredArgsConstructor
class DoctorPersistenceAdapter implements DoctorRepositoryPort {

    private final DoctorJpaRepository repository;

    @Override
    public Doctor save(Doctor doctor) {
        DoctorEntity entity = DoctorEntity.builder()
                .id(doctor.getId())
                .userId(doctor.getUserId())
                .specialization(doctor.getSpecialization())
                .createdAt(doctor.getCreatedAt())
                .build();
        entity = repository.save(entity);
        return Doctor.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .specialization(entity.getSpecialization())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    public Optional<Doctor> findById(String id) {
        return repository.findById(id).map(e -> Doctor.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .specialization(e.getSpecialization())
                .createdAt(e.getCreatedAt())
                .build());
    }

    @Override
    public Optional<Doctor> findByUserId(String userId) {
        return repository.findByUserId(userId).map(e -> Doctor.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .specialization(e.getSpecialization())
                .createdAt(e.getCreatedAt())
                .build());
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }
}

@Component
@RequiredArgsConstructor
class PatientPersistenceAdapter implements PatientRepositoryPort {

    private final PatientJpaRepository repository;

    @Override
    public Patient save(Patient patient) {
        PatientEntity entity = PatientEntity.builder()
                .id(patient.getId())
                .userId(patient.getUserId())
                .phone(patient.getPhone())
                .createdAt(patient.getCreatedAt())
                .build();
        entity = repository.save(entity);
        return Patient.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .phone(entity.getPhone())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    public Optional<Patient> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Patient> findByUserId(String userId) {
        return repository.findByUserId(userId).map(this::toDomain);
    }

    private Patient toDomain(PatientEntity entity) {
        return Patient.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .phone(entity.getPhone())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

@Component
@RequiredArgsConstructor
class SlotPersistenceAdapter implements SlotRepositoryPort {

    private final SlotJpaRepository repository;

    @Override
    public Optional<Slot> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Slot> findAvailableByDoctorAndDate(String doctorId, LocalDate date) {
        Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return repository.findAvailableByDoctorAndDateRange(doctorId, startOfDay, endOfDay)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public Slot reserveSlot(String slotId, int expectedVersion) {
        SlotEntity entity = repository.findById(slotId)
                .orElseThrow(() -> new SlotAlreadyBookedException());

        if (!entity.isAvailable() || entity.getVersion() != expectedVersion) {
            throw new SlotAlreadyBookedException();
        }

        entity.setAvailable(false);
        try {
            entity = repository.save(entity);
        } catch (OptimisticLockingFailureException e) {
            throw new SlotAlreadyBookedException();
        }
        return toDomain(entity);
    }

    @Override
    @Transactional
    public void releaseSlot(String slotId) {
        repository.findById(slotId).ifPresent(entity -> {
            entity.setAvailable(true);
            repository.save(entity);
        });
    }

    private Slot toDomain(SlotEntity entity) {
        return Slot.builder()
                .id(entity.getId())
                .doctorId(entity.getDoctorId())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .available(entity.isAvailable())
                .version(entity.getVersion())
                .build();
    }
}

@Component
@RequiredArgsConstructor
class AppointmentPersistenceAdapter implements AppointmentRepositoryPort {

    private final AppointmentJpaRepository repository;

    @Override
    public Appointment save(Appointment appointment) {
        return toDomain(repository.save(toEntity(appointment)));
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Appointment> findByPatientId(String patientId, AppointmentStatus status, int page, int size) {
        return repository.findByPatientIdAndStatus(patientId, status, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countByPatientId(String patientId, AppointmentStatus status) {
        return repository.countByPatientIdAndStatus(patientId, status);
    }

    @Override
    public List<Appointment> findByDoctorId(String doctorId, AppointmentStatus status, int page, int size) {
        return repository.findByDoctorIdAndStatus(doctorId, status, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countByDoctorId(String doctorId, AppointmentStatus status) {
        return repository.countByDoctorIdAndStatus(doctorId, status);
    }

    private AppointmentEntity toEntity(Appointment appointment) {
        return AppointmentEntity.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatientId())
                .doctorId(appointment.getDoctorId())
                .slotId(appointment.getSlotId())
                .status(appointment.getStatus())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }

    private Appointment toDomain(AppointmentEntity entity) {
        return Appointment.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .doctorId(entity.getDoctorId())
                .slotId(entity.getSlotId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

@Component
@RequiredArgsConstructor
class AppointmentLogPersistenceAdapter implements AppointmentLogRepositoryPort {

    private final AppointmentLogJpaRepository repository;

    @Override
    public AppointmentLog save(AppointmentLog log) {
        AppointmentLogEntity entity = AppointmentLogEntity.builder()
                .id(log.getId())
                .appointmentId(log.getAppointmentId())
                .eventType(log.getEventType())
                .oldStatus(log.getOldStatus())
                .newStatus(log.getNewStatus())
                .actor(log.getActor())
                .message(log.getMessage())
                .createdAt(log.getCreatedAt())
                .build();
        entity = repository.save(entity);
        return AppointmentLog.builder()
                .id(entity.getId())
                .appointmentId(entity.getAppointmentId())
                .eventType(entity.getEventType())
                .oldStatus(entity.getOldStatus())
                .newStatus(entity.getNewStatus())
                .actor(entity.getActor())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    public List<AppointmentLog> findByAppointmentId(String appointmentId) {
        return repository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId).stream()
                .map(e -> AppointmentLog.builder()
                        .id(e.getId())
                        .appointmentId(e.getAppointmentId())
                        .eventType(e.getEventType())
                        .oldStatus(e.getOldStatus())
                        .newStatus(e.getNewStatus())
                        .actor(e.getActor())
                        .message(e.getMessage())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
    }
}

@Component
@RequiredArgsConstructor
class OutboxPersistenceAdapter implements OutboxRepositoryPort {

    private final OutboxJpaRepository repository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity entity = OutboxEventEntity.builder()
                .id(event.getId())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .sentAt(event.getSentAt())
                .build();
        entity = repository.save(entity);
        return toDomain(entity);
    }

    @Override
    public List<OutboxEvent> findPendingEvents(int limit) {
        return repository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void markAsSent(String eventId) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.setStatus(OutboxEventStatus.SENT);
            entity.setSentAt(Instant.now());
            repository.save(entity);
        });
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        return OutboxEvent.builder()
                .id(entity.getId())
                .aggregateId(entity.getAggregateId())
                .eventType(entity.getEventType())
                .payload(entity.getPayload())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }
}

@Component
@RequiredArgsConstructor
class NotificationStatusPersistenceAdapter implements NotificationStatusRepositoryPort {

    private final NotificationStatusJpaRepository repository;

    @Override
    public NotificationStatusRecord save(NotificationStatusRecord record) {
        NotificationStatusEntity entity = NotificationStatusEntity.builder()
                .id(record.getId())
                .appointmentId(record.getAppointmentId())
                .channel(record.getChannel())
                .status(record.getStatus())
                .attemptedAt(record.getAttemptedAt())
                .processedAt(record.getProcessedAt())
                .build();
        entity = repository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<NotificationStatusRecord> findByAppointmentId(String appointmentId) {
        return repository.findByAppointmentId(appointmentId).map(this::toDomain);
    }

    private NotificationStatusRecord toDomain(NotificationStatusEntity entity) {
        return NotificationStatusRecord.builder()
                .id(entity.getId())
                .appointmentId(entity.getAppointmentId())
                .channel(entity.getChannel())
                .status(entity.getStatus())
                .attemptedAt(entity.getAttemptedAt())
                .processedAt(entity.getProcessedAt())
                .build();
    }
}

@Component
@RequiredArgsConstructor
class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;

    @Override
    public void save(String userId, String tokenHash, Instant expiresAt) {
        repository.save(RefreshTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public Optional<StoredRefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash)
                .map(e -> new StoredRefreshToken(e.getId(), e.getUserId(), e.getTokenHash(),
                        e.getExpiresAt(), e.isRevoked()));
    }

    @Override
    public void revoke(String tokenId) {
        repository.findById(tokenId).ifPresent(e -> {
            e.setRevoked(true);
            repository.save(e);
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(String userId) {
        repository.revokeAllByUserId(userId);
    }
}

@Component
@RequiredArgsConstructor
class IdempotencyPersistenceAdapter implements IdempotencyStorePort {

    private final IdempotencyKeyJpaRepository repository;

    @Override
    public Optional<StoredResponse> findByKey(String key) {
        return repository.findByIdempotencyKeyAndExpiresAtAfter(key, Instant.now())
                .map(e -> new StoredResponse(e.getResponseBody(), e.getStatusCode()));
    }

    @Override
    public void store(String key, String responseBody, int statusCode, Instant expiresAt) {
        repository.save(IdempotencyKeyEntity.builder()
                .id(UUID.randomUUID().toString())
                .idempotencyKey(key)
                .responseBody(responseBody)
                .statusCode(statusCode)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build());
    }
}
