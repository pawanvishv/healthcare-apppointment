package com.healthapp.backend.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.backend.application.dto.PageResponse;
import com.healthapp.backend.application.dto.request.BookAppointmentRequest;
import com.healthapp.backend.application.dto.request.RescheduleAppointmentRequest;
import com.healthapp.backend.application.dto.response.AppointmentLogResponse;
import com.healthapp.backend.application.dto.response.AppointmentResponse;
import com.healthapp.backend.application.dto.response.SlotResponse;
import com.healthapp.backend.application.port.out.*;
import com.healthapp.backend.domain.exception.IllegalStateTransitionException;
import com.healthapp.backend.domain.exception.ResourceNotFoundException;
import com.healthapp.backend.domain.exception.SlotAlreadyBookedException;
import com.healthapp.backend.domain.exception.UnauthorizedAccessException;
import com.healthapp.backend.domain.model.*;
import com.healthapp.backend.domain.service.CancellationPolicy;
import com.healthapp.backend.domain.service.SlotAvailabilityPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final SlotRepositoryPort slotRepository;
    private final AppointmentRepositoryPort appointmentRepository;
    private final AppointmentLogRepositoryPort appointmentLogRepository;
    private final DoctorRepositoryPort doctorRepository;
    private final PatientRepositoryPort patientRepository;
    private final OutboxRepositoryPort outboxRepository;
    private final NotificationStatusRepositoryPort notificationStatusRepository;
    private final IdempotencyStorePort idempotencyStore;
    private final ObjectMapper objectMapper;
    private final SlotAvailabilityPolicy slotAvailabilityPolicy;
    private final CancellationPolicy cancellationPolicy;

    @Value("${app.kafka.topic.appointment-booked}")
    private String appointmentBookedTopic;

    @Value("${app.kafka.topic.appointment-cancelled}")
    private String appointmentCancelledTopic;

    @Value("${app.kafka.topic.appointment-rescheduled}")
    private String appointmentRescheduledTopic;

    @Value("${app.idempotency.ttl-hours:24}")
    private int idempotencyTtlHours;

    @Cacheable(value = "availableSlots", key = "#doctorId + '-' + #date")
    public List<SlotResponse> getAvailableSlots(String doctorId, LocalDate date) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor", doctorId);
        }
        return slotRepository.findAvailableByDoctorAndDate(doctorId, date).stream()
                .map(this::toSlotResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "availableSlots", allEntries = true)
    public AppointmentResponse bookAppointment(String userId, BookAppointmentRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var cached = idempotencyStore.findByKey(idempotencyKey);
            if (cached.isPresent()) {
                try {
                    return objectMapper.readValue(cached.get().responseBody(), AppointmentResponse.class);
                } catch (Exception e) {
                    log.warn("Failed to deserialize idempotent response for key {}", idempotencyKey);
                }
            }
        }

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile", userId));

        if (!doctorRepository.existsById(request.getDoctorId())) {
            throw new ResourceNotFoundException("Doctor", request.getDoctorId());
        }

        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot", request.getSlotId()));

        slotAvailabilityPolicy.validateBelongsToDoctor(slot, request.getDoctorId());
        slotAvailabilityPolicy.validateBookable(slot, Instant.now());

        Slot reserved;
        try {
            reserved = slotRepository.reserveSlot(slot.getId(), slot.getVersion());
        } catch (SlotAlreadyBookedException e) {
            throw e;
        }

        Instant now = Instant.now();
        String appointmentId = UUID.randomUUID().toString();

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patientId(patient.getId())
                .doctorId(request.getDoctorId())
                .slotId(reserved.getId())
                .status(AppointmentStatus.CONFIRMED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        appointmentRepository.save(appointment);

        appointmentLogRepository.save(AppointmentLog.builder()
                .id(UUID.randomUUID().toString())
                .appointmentId(appointmentId)
                .eventType("CREATED")
                .oldStatus(null)
                .newStatus(AppointmentStatus.CONFIRMED)
                .actor(userId)
                .message("Appointment booked")
                .createdAt(now)
                .build());

        notificationStatusRepository.save(NotificationStatusRecord.builder()
                .id(UUID.randomUUID().toString())
                .appointmentId(appointmentId)
                .channel("EMAIL")
                .status(ProcessingStatus.PENDING)
                .build());

        saveOutboxEvent(appointmentId, appointmentBookedTopic, buildBookedEventPayload(appointment, reserved));

        AppointmentResponse response = toAppointmentResponse(appointment, reserved, ProcessingStatus.PENDING);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String body = objectMapper.writeValueAsString(response);
                idempotencyStore.store(idempotencyKey, body, 201,
                        Instant.now().plusSeconds(idempotencyTtlHours * 3600L));
            } catch (Exception e) {
                log.warn("Failed to store idempotent response", e);
            }
        }

        log.info("Appointment booked: id={}, patient={}, slot={}", appointmentId, patient.getId(), reserved.getId());
        return response;
    }

    public PageResponse<AppointmentResponse> getMyAppointments(String userId, Role role,
                                                                AppointmentStatus status,
                                                                int page, int size) {
        List<Appointment> appointments;
        long total;

        if (role == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile", userId));
            appointments = appointmentRepository.findByDoctorId(doctor.getId(), status, page, size);
            total = appointmentRepository.countByDoctorId(doctor.getId(), status);
        } else {
            Patient patient = patientRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile", userId));
            appointments = appointmentRepository.findByPatientId(patient.getId(), status, page, size);
            total = appointmentRepository.countByPatientId(patient.getId(), status);
        }

        List<AppointmentResponse> content = appointments.stream()
                .map(this::toAppointmentResponseWithDetails)
                .toList();

        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        return PageResponse.<AppointmentResponse>builder()
                .content(content)
                .totalElements(total)
                .totalPages(totalPages)
                .page(page)
                .size(size)
                .build();
    }

    public AppointmentResponse getAppointment(String userId, Role role, String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        validateAccess(userId, role, appointment);
        return toAppointmentResponseWithDetails(appointment);
    }

    @Transactional
    @CacheEvict(value = "availableSlots", allEntries = true)
    public AppointmentResponse cancelAppointment(String userId, Role role, String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        validateAccess(userId, role, appointment);

        Slot slot = slotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot", appointment.getSlotId()));

        cancellationPolicy.validateCancellable(appointment, slot.getStartTime(), Instant.now());

        slotRepository.releaseSlot(slot.getId());

        Instant now = Instant.now();
        Appointment cancelled = appointment.toBuilder()
                .status(AppointmentStatus.CANCELLED)
                .updatedAt(now)
                .build();

        appointmentRepository.save(cancelled);

        appointmentLogRepository.save(AppointmentLog.builder()
                .id(UUID.randomUUID().toString())
                .appointmentId(appointmentId)
                .eventType("CANCELLED")
                .oldStatus(AppointmentStatus.CONFIRMED)
                .newStatus(AppointmentStatus.CANCELLED)
                .actor(userId)
                .message("Appointment cancelled")
                .createdAt(now)
                .build());

        saveOutboxEvent(appointmentId, appointmentCancelledTopic, buildCancelledEventPayload(cancelled));

        log.info("Appointment cancelled: id={}", appointmentId);
        return toAppointmentResponseWithDetails(cancelled);
    }

    @Transactional
    @CacheEvict(value = "availableSlots", allEntries = true)
    public AppointmentResponse rescheduleAppointment(String userId, String appointmentId,
                                                      RescheduleAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile", userId));

        if (!appointment.getPatientId().equals(patient.getId())) {
            throw new UnauthorizedAccessException();
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateTransitionException("Only confirmed appointments can be rescheduled");
        }

        Slot oldSlot = slotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot", appointment.getSlotId()));

        Slot newSlot = slotRepository.findById(request.getNewSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot", request.getNewSlotId()));

        slotAvailabilityPolicy.validateBelongsToDoctor(newSlot, appointment.getDoctorId());
        slotAvailabilityPolicy.validateBookable(newSlot, Instant.now());

        Slot reserved = slotRepository.reserveSlot(newSlot.getId(), newSlot.getVersion());
        slotRepository.releaseSlot(oldSlot.getId());

        Instant now = Instant.now();
        Appointment rescheduled = appointment.toBuilder()
                .slotId(reserved.getId())
                .status(AppointmentStatus.CONFIRMED)
                .updatedAt(now)
                .build();

        appointmentRepository.save(rescheduled);

        appointmentLogRepository.save(AppointmentLog.builder()
                .id(UUID.randomUUID().toString())
                .appointmentId(appointmentId)
                .eventType("RESCHEDULED")
                .oldStatus(AppointmentStatus.CONFIRMED)
                .newStatus(AppointmentStatus.CONFIRMED)
                .actor(userId)
                .message("Rescheduled from " + oldSlot.getId() + " to " + reserved.getId())
                .createdAt(now)
                .build());

        notificationStatusRepository.findByAppointmentId(appointmentId)
                .ifPresentOrElse(
                        existing -> notificationStatusRepository.save(existing.toBuilder()
                                .status(ProcessingStatus.PENDING)
                                .attemptedAt(null)
                                .processedAt(null)
                                .build()),
                        () -> notificationStatusRepository.save(NotificationStatusRecord.builder()
                                .id(UUID.randomUUID().toString())
                                .appointmentId(appointmentId)
                                .channel("EMAIL")
                                .status(ProcessingStatus.PENDING)
                                .build()));

        saveOutboxEvent(appointmentId, appointmentRescheduledTopic,
                buildRescheduledEventPayload(rescheduled, oldSlot.getId(), reserved.getId()));

        log.info("Appointment rescheduled: id={}, newSlot={}", appointmentId, reserved.getId());
        return toAppointmentResponse(rescheduled, reserved, ProcessingStatus.PENDING);
    }

    public List<AppointmentLogResponse> getAppointmentHistory(String userId, Role role, String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        validateAccess(userId, role, appointment);

        return appointmentLogRepository.findByAppointmentId(appointmentId).stream()
                .map(entry -> AppointmentLogResponse.builder()
                        .id(entry.getId())
                        .eventType(entry.getEventType())
                        .oldStatus(entry.getOldStatus())
                        .newStatus(entry.getNewStatus())
                        .actor(entry.getActor())
                        .message(entry.getMessage())
                        .createdAt(entry.getCreatedAt())
                        .build())
                .toList();
    }

    private void validateAccess(String userId, Role role, Appointment appointment) {
        if (role == Role.ADMIN || role == Role.DOCTOR) {
            return;
        }
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile", userId));
        if (!appointment.getPatientId().equals(patient.getId())) {
            throw new UnauthorizedAccessException();
        }
    }

    private AppointmentResponse toAppointmentResponseWithDetails(Appointment appointment) {
        Slot slot = slotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot", appointment.getSlotId()));
        ProcessingStatus processingStatus = notificationStatusRepository.findByAppointmentId(appointment.getId())
                .map(NotificationStatusRecord::getStatus)
                .orElse(ProcessingStatus.PENDING);
        return toAppointmentResponse(appointment, slot, processingStatus);
    }

    private AppointmentResponse toAppointmentResponse(Appointment appointment, Slot slot,
                                                       ProcessingStatus processingStatus) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .status(appointment.getStatus())
                .processingStatus(processingStatus)
                .doctorId(appointment.getDoctorId())
                .slotId(appointment.getSlotId())
                .patientId(appointment.getPatientId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    private SlotResponse toSlotResponse(Slot slot) {
        return SlotResponse.builder()
                .id(slot.getId())
                .doctorId(slot.getDoctorId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .available(slot.isAvailable())
                .build();
    }

    private void saveOutboxEvent(String aggregateId, String eventType, Map<String, Object> payload) {
        try {
            outboxRepository.save(OutboxEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(OutboxEventStatus.PENDING)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }

    private Map<String, Object> buildBookedEventPayload(Appointment appointment, Slot slot) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("appointmentId", appointment.getId());
        payload.put("patientId", appointment.getPatientId());
        payload.put("doctorId", appointment.getDoctorId());
        payload.put("slotId", slot.getId());
        payload.put("startTime", slot.getStartTime().toString());
        payload.put("status", appointment.getStatus().name());
        return payload;
    }

    private Map<String, Object> buildCancelledEventPayload(Appointment appointment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("appointmentId", appointment.getId());
        payload.put("patientId", appointment.getPatientId());
        payload.put("doctorId", appointment.getDoctorId());
        payload.put("status", appointment.getStatus().name());
        return payload;
    }

    private Map<String, Object> buildRescheduledEventPayload(Appointment appointment,
                                                             String oldSlotId, String newSlotId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("appointmentId", appointment.getId());
        payload.put("patientId", appointment.getPatientId());
        payload.put("doctorId", appointment.getDoctorId());
        payload.put("oldSlotId", oldSlotId);
        payload.put("newSlotId", newSlotId);
        payload.put("status", appointment.getStatus().name());
        return payload;
    }
}
