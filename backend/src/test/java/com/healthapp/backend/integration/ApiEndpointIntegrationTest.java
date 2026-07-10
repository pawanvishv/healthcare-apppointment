package com.healthapp.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiEndpointIntegrationTest {

    private static final String PATIENT_EMAIL = "patient@healthapp.com";
    private static final String DOCTOR_EMAIL = "doctor@healthapp.com";
    private static final String PASSWORD = "Password123!";
    private static final String DOCTOR_ID = "d-101";
    private static final String SLOT_DATE = "2026-07-10";
    private static final String BOOK_SLOT_ID = "s-003";
    private static final String RESCHEDULE_SLOT_ID = "s-004";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String patientAccessToken;
    private String patientRefreshToken;
    private String doctorAccessToken;
    private String appointmentId;
    private String idempotencyKey = "integration-test-key-" + System.currentTimeMillis();

    // ── Actuator ──────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("GET /actuator/health – returns UP")
    void healthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/auth/login – patient login succeeds")
    void loginPatient() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(PATIENT_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andReturn();

        JsonNode data = parseData(result);
        patientAccessToken = data.get("accessToken").asText();
        patientRefreshToken = data.get("refreshToken").asText();
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/v1/auth/login – invalid credentials returns 401")
    void loginInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}
                                """.formatted(PATIENT_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/v1/auth/register – new patient registration succeeds")
    void registerNewPatient() throws Exception {
        String uniqueEmail = "testpatient-" + System.currentTimeMillis() + "@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","role":"PATIENT","phone":"+9876543210"}
                                """.formatted(uniqueEmail, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/v1/auth/login – doctor login succeeds")
    void loginDoctor() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(DOCTOR_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        doctorAccessToken = parseData(result).get("accessToken").asText();
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/v1/auth/refresh – returns new tokens")
    void refreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(patientRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    // ── Slots ─────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("GET /api/v1/slots – without token returns 401 or 403")
    void getSlotsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/slots")
                        .param("doctorId", DOCTOR_ID)
                        .param("date", SLOT_DATE))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/v1/slots – with token returns available slots")
    void getSlotsWithAuth() throws Exception {
        mockMvc.perform(get("/api/v1/slots")
                        .param("doctorId", DOCTOR_ID)
                        .param("date", SLOT_DATE)
                        .header("Authorization", bearer(patientAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].doctorId").value(DOCTOR_ID))
                .andExpect(jsonPath("$.data[0].available").value(true));
    }

    // ── Appointments – booking ────────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("POST /api/v1/appointments – doctor role returns 403")
    void bookAppointmentAsDoctorForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", bearer(doctorAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doctorId":"%s","slotId":"%s"}
                                """.formatted(DOCTOR_ID, BOOK_SLOT_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(21)
    @DisplayName("POST /api/v1/appointments – without token returns 401 or 403")
    void bookAppointmentWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doctorId":"%s","slotId":"%s"}
                                """.formatted(DOCTOR_ID, BOOK_SLOT_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(22)
    @DisplayName("POST /api/v1/appointments – patient books successfully")
    void bookAppointmentAsPatient() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", bearer(patientAccessToken))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doctorId":"%s","slotId":"%s"}
                                """.formatted(DOCTOR_ID, BOOK_SLOT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.processingStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.doctorId").value(DOCTOR_ID))
                .andExpect(jsonPath("$.data.slotId").value(BOOK_SLOT_ID))
                .andReturn();

        appointmentId = parseData(result).get("id").asText();
    }

    @Test
    @Order(23)
    @DisplayName("POST /api/v1/appointments – idempotency key returns same result")
    void bookAppointmentIdempotency() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", bearer(patientAccessToken))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doctorId":"%s","slotId":"%s"}
                                """.formatted(DOCTOR_ID, BOOK_SLOT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(appointmentId));
    }

    @Test
    @Order(24)
    @DisplayName("POST /api/v1/appointments – already booked slot returns 409")
    void bookAlreadyBookedSlot() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", bearer(patientAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doctorId":"%s","slotId":"%s"}
                                """.formatted(DOCTOR_ID, BOOK_SLOT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("SLOT_UNAVAILABLE"));
    }

    // ── Appointments – read ───────────────────────────────────────────────────

    @Test
    @Order(30)
    @DisplayName("GET /api/v1/appointments/me – returns patient's appointments")
    void getMyAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/me")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", bearer(patientAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(appointmentId))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(31)
    @DisplayName("GET /api/v1/appointments/{id} – returns appointment details")
    void getAppointmentById() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/{id}", appointmentId)
                        .header("Authorization", bearer(patientAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(appointmentId))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.doctorId").value(DOCTOR_ID));
    }

    @Test
    @Order(32)
    @DisplayName("GET /api/v1/appointments/{id}/history – returns audit log")
    void getAppointmentHistory() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/{id}/history", appointmentId)
                        .header("Authorization", bearer(patientAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].eventType").value("CREATED"));
    }

    // ── Appointments – update ─────────────────────────────────────────────────

    @Test
    @Order(40)
    @DisplayName("PATCH /api/v1/appointments/{id}/reschedule – moves to new slot")
    void rescheduleAppointment() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/{id}/reschedule", appointmentId)
                        .header("Authorization", bearer(patientAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newSlotId":"%s"}
                                """.formatted(RESCHEDULE_SLOT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(appointmentId))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.slotId").value(RESCHEDULE_SLOT_ID));
    }

    @Test
    @Order(41)
    @DisplayName("PATCH /api/v1/appointments/{id}/cancel – cancels appointment")
    void cancelAppointment() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appointmentId)
                        .header("Authorization", bearer(patientAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(appointmentId))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ── Auth – logout ─────────────────────────────────────────────────────────

    @Test
    @Order(50)
    @DisplayName("POST /api/v1/auth/logout – revokes refresh token")
    void logout() throws Exception {
        // Re-login to get a fresh refresh token for logout test
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(PATIENT_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = parseData(loginResult).get("refreshToken").asText();
        String accessToken = parseData(loginResult).get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JsonNode parseData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
