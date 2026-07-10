package com.healthapp.backend.adapter.in.web.controller;

import com.healthapp.backend.adapter.in.web.filter.JwtAuthenticationFilter;
import com.healthapp.backend.application.dto.ApiResponse;
import com.healthapp.backend.application.dto.PageResponse;
import com.healthapp.backend.application.dto.request.BookAppointmentRequest;
import com.healthapp.backend.application.dto.request.RescheduleAppointmentRequest;
import com.healthapp.backend.application.dto.response.AppointmentLogResponse;
import com.healthapp.backend.application.dto.response.AppointmentResponse;
import com.healthapp.backend.application.dto.response.SlotResponse;
import com.healthapp.backend.application.service.AppointmentService;
import com.healthapp.backend.domain.model.AppointmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment and slot management")
@SecurityRequirement(name = "Bearer Authentication")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/slots")
    @Operation(summary = "Get available slots for a doctor on a date")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> getAvailableSlots(
            @RequestParam String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getAvailableSlots(doctorId, date)));
    }

    @PostMapping("/appointments")
    @Operation(summary = "Book an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(
            @Valid @RequestBody BookAppointmentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String userId = JwtAuthenticationFilter.getCurrentUserId();
        AppointmentResponse response = appointmentService.bookAppointment(userId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/appointments/me")
    @Operation(summary = "Get current user's appointments")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getMyAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = JwtAuthenticationFilter.getCurrentUserId();
        var role = JwtAuthenticationFilter.getCurrentUserRole();
        return ResponseEntity.ok(ApiResponse.ok(
                appointmentService.getMyAppointments(userId, role, status, page, size)));
    }

    @GetMapping("/appointments/{id}")
    @Operation(summary = "Get appointment details")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(@PathVariable String id) {
        String userId = JwtAuthenticationFilter.getCurrentUserId();
        var role = JwtAuthenticationFilter.getCurrentUserRole();
        return ResponseEntity.ok(ApiResponse.ok(
                appointmentService.getAppointment(userId, role, id)));
    }

    @PatchMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(@PathVariable String id) {
        String userId = JwtAuthenticationFilter.getCurrentUserId();
        var role = JwtAuthenticationFilter.getCurrentUserRole();
        return ResponseEntity.ok(ApiResponse.ok(
                appointmentService.cancelAppointment(userId, role, id)));
    }

    @PatchMapping("/appointments/{id}/reschedule")
    @Operation(summary = "Reschedule an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rescheduleAppointment(
            @PathVariable String id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        String userId = JwtAuthenticationFilter.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                appointmentService.rescheduleAppointment(userId, id, request)));
    }

    @GetMapping("/appointments/{id}/history")
    @Operation(summary = "Get appointment audit history")
    public ResponseEntity<ApiResponse<List<AppointmentLogResponse>>> getAppointmentHistory(
            @PathVariable String id) {
        String userId = JwtAuthenticationFilter.getCurrentUserId();
        var role = JwtAuthenticationFilter.getCurrentUserRole();
        return ResponseEntity.ok(ApiResponse.ok(
                appointmentService.getAppointmentHistory(userId, role, id)));
    }
}
