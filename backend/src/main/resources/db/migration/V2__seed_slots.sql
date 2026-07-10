-- Seed a doctor user and sample slots for development
INSERT INTO users (id, email, password_hash, role, created_at, updated_at) VALUES
('u-doctor-1', 'doctor@healthapp.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oYgVqF5K5K5K', 'DOCTOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('u-patient-1', 'patient@healthapp.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.G2oYgVqF5K5K5K', 'PATIENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Password for both seed users: Password123!
-- BCrypt hash will be replaced at runtime; using placeholder for schema seed

INSERT INTO doctors (id, user_id, specialization, created_at) VALUES
('d-101', 'u-doctor-1', 'General Practice', CURRENT_TIMESTAMP);

INSERT INTO patients (id, user_id, phone, created_at) VALUES
('p-201', 'u-patient-1', '+1234567890', CURRENT_TIMESTAMP);

-- Generate slots for the next 7 days, 9 AM - 5 PM, 30-min intervals
INSERT INTO slots (id, doctor_id, start_time, end_time, available, version) VALUES
('s-001', 'd-101', TIMESTAMP '2026-07-10 09:00:00', TIMESTAMP '2026-07-10 09:30:00', TRUE, 0),
('s-002', 'd-101', TIMESTAMP '2026-07-10 09:30:00', TIMESTAMP '2026-07-10 10:00:00', TRUE, 0),
('s-003', 'd-101', TIMESTAMP '2026-07-10 10:00:00', TIMESTAMP '2026-07-10 10:30:00', TRUE, 0),
('s-004', 'd-101', TIMESTAMP '2026-07-10 10:30:00', TIMESTAMP '2026-07-10 11:00:00', TRUE, 0),
('s-005', 'd-101', TIMESTAMP '2026-07-10 11:00:00', TIMESTAMP '2026-07-10 11:30:00', TRUE, 0),
('s-006', 'd-101', TIMESTAMP '2026-07-10 14:00:00', TIMESTAMP '2026-07-10 14:30:00', TRUE, 0),
('s-007', 'd-101', TIMESTAMP '2026-07-10 14:30:00', TIMESTAMP '2026-07-10 15:00:00', TRUE, 0),
('s-008', 'd-101', TIMESTAMP '2026-07-10 15:00:00', TIMESTAMP '2026-07-10 15:30:00', TRUE, 0),
('s-009', 'd-101', TIMESTAMP '2026-07-11 09:00:00', TIMESTAMP '2026-07-11 09:30:00', TRUE, 0),
('s-010', 'd-101', TIMESTAMP '2026-07-11 09:30:00', TIMESTAMP '2026-07-11 10:00:00', TRUE, 0),
('s-011', 'd-101', TIMESTAMP '2026-07-11 10:00:00', TIMESTAMP '2026-07-11 10:30:00', TRUE, 0),
('s-012', 'd-101', TIMESTAMP '2026-07-11 14:00:00', TIMESTAMP '2026-07-11 14:30:00', TRUE, 0);
