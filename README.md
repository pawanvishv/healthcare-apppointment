# Healthcare Appointment Platform

Full-stack appointment management with event-driven notifications.

## Components

| Service | Tech | Port | Role |
|---|---|---|---|
| **Backend** | Spring Boot 3.3 / Java 21 | 8080 | REST API, auth, appointments, Kafka producer |
| **Frontend** | Next.js 14 / TypeScript | 3000 | Patient & doctor UI |
| **Worker** | Python 3.11 | — | Kafka consumer, email notifications, H2 DB updates |
| **Kafka** | Confluent 7.5 | 9092 | Event bus |
| **Database** | H2 (file) | 9093 (TCP) | Shared DB — backend + Python worker |

## Evaluation criteria coverage

| Requirement | Status |
|---|---|
| Spring Boot backend | ✅ `backend/` |
| Python worker | ✅ `worker/` |
| Kafka event-driven workflow | ✅ Outbox → Kafka → Python consumer |
| Database integration | ✅ Shared H2 database |
| Authentication & authorization | ✅ JWT (patient/doctor roles) |
| Clean architecture | ✅ Hexagonal backend, layered worker |
| Appointment workflow | ✅ Book, cancel, reschedule, slot browse |

## Event flow

1. Patient books appointment via Spring Boot API
2. Backend writes `notification_status` = `PENDING` and outbox event
3. `OutboxPoller` publishes to Kafka topic `appointment.booked`
4. Python worker consumes event, sends notification, updates H2 → `NOTIFIED`
5. Frontend polls appointment detail until `processingStatus` updates

### Kafka topics

- `appointment.booked`
- `appointment.cancelled`
- `appointment.rescheduled`

## Quick start (full stack with H2 + Kafka)

### 1. Kafka

```bash
docker compose up -d
```

### 2. Backend (H2 file DB + Kafka)

```bash
cd backend
mvn dependency:copy-dependencies -DincludeArtifactIds=h2 -q
set KAFKA_ENABLED=true
mvn spring-boot:run
```

API: http://localhost:8080/swagger-ui.html  
H2 Console: http://localhost:8080/h2-console (JDBC: `jdbc:h2:file:./data/appointments`, user `sa`)

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

App: http://localhost:3000

### 4. Python worker

Requires Python 3.11+ and Java (for H2 JDBC).

```bash
cd worker
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
python main.py
```

Worker connects to H2 via `jdbc:h2:tcp://localhost:9093/./data/appointments`.

## Dev mode (without Kafka)

For UI-only development, run backend with default `dev` profile (Kafka disabled). Events are logged but not published; `processingStatus` stays `PENDING`.

```bash
cd backend
mvn spring-boot:run
```

## Seed credentials

| Role | Email | Password |
|---|---|---|
| Patient | `patient@healthapp.com` | `Password123!` |
| Doctor | `doctor@healthapp.com` | `Password123!` |

Doctor ID: `d-101` | Sample dates: `2026-07-10`, `2026-07-11`

## Documentation

- [Backend guide](backend/PROJECT_GUIDE.md)
- [Frontend guide](frontend/PROJECT_GUIDE.md)
- [Worker guide](worker/README.md)
