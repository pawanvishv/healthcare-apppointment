-- Users and auth
CREATE TABLE users (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE refresh_tokens (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- Doctors and patients
CREATE TABLE doctors (
    id              VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    specialization  VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL
);

CREATE TABLE patients (
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    phone       VARCHAR(20),
    created_at  TIMESTAMP NOT NULL
);

-- Slots
CREATE TABLE slots (
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    doctor_id   VARCHAR(36) NOT NULL REFERENCES doctors(id) ON DELETE RESTRICT,
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP NOT NULL,
    available   BOOLEAN NOT NULL DEFAULT TRUE,
    version     INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_slots_doctor_start UNIQUE (doctor_id, start_time)
);

CREATE INDEX idx_slots_doctor_start ON slots(doctor_id, start_time);

-- Appointments
CREATE TABLE appointments (
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    patient_id  VARCHAR(36) NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    doctor_id   VARCHAR(36) NOT NULL REFERENCES doctors(id) ON DELETE RESTRICT,
    slot_id     VARCHAR(36) NOT NULL REFERENCES slots(id) ON DELETE RESTRICT,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor ON appointments(doctor_id);
CREATE INDEX idx_appointments_slot ON appointments(slot_id);

-- Appointment audit log
CREATE TABLE appointment_logs (
    id              VARCHAR(36) NOT NULL PRIMARY KEY,
    appointment_id  VARCHAR(36) NOT NULL REFERENCES appointments(id) ON DELETE RESTRICT,
    event_type      VARCHAR(50) NOT NULL,
    old_status      VARCHAR(20),
    new_status      VARCHAR(20),
    actor           VARCHAR(255),
    message         VARCHAR(500),
    created_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_appointment_logs_appointment ON appointment_logs(appointment_id);

-- Transactional outbox
CREATE TABLE outbox_events (
    id              VARCHAR(36) NOT NULL PRIMARY KEY,
    aggregate_id    VARCHAR(36) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         CLOB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL,
    sent_at         TIMESTAMP
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);

-- Notification processing status (updated by Python worker)
CREATE TABLE notification_status (
    id              VARCHAR(36) NOT NULL PRIMARY KEY,
    appointment_id  VARCHAR(36) NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE RESTRICT,
    channel         VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempted_at    TIMESTAMP,
    processed_at    TIMESTAMP
);

-- Idempotency keys for booking retries
CREATE TABLE idempotency_keys (
    id              VARCHAR(36) NOT NULL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    response_body   CLOB NOT NULL,
    status_code     INT NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    expires_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);
