# Healthcare Notification Worker

Python service that consumes appointment events from Kafka, sends patient notifications, and updates `notification_status` in the **shared H2 database**.

## Architecture

```
Spring Boot (producer)          Kafka                 Python Worker (consumer)
─────────────────────    ───────────────────    ─────────────────────────────
Book / Cancel / Reschedule  appointment.booked      → send email
       │                    appointment.cancelled   → log / SMTP
       ▼                    appointment.rescheduled → update notification_status
  H2 file DB (AUTO_SERVER) ──publish──►  (3 topics)  → NOTIFIED / FAILED
  notification_status (PENDING)              │
                                             └── JDBC tcp://localhost:9093
```

## Prerequisites

- Python 3.11+
- Java 21 (for H2 JDBC driver via JPype — same JDK as Spring Boot)
- Docker (for Kafka only)
- Spring Boot backend running with H2 `AUTO_SERVER` on port **9093**

## Quick start

### 1. Start Kafka

```bash
docker compose up -d
```

### 2. Prepare H2 JDBC driver (one-time)

```bash
cd backend
mvn dependency:copy-dependencies -DincludeArtifactIds=h2 -q
```

Or the worker auto-detects the JAR from your local Maven cache (`~/.m2/repository/com/h2database/h2/`).

### 3. Start backend (H2 + Kafka)

```bash
cd backend
set KAFKA_ENABLED=true
mvn spring-boot:run
```

H2 file DB: `backend/data/appointments.mv.db`  
H2 TCP server: `localhost:9093` (for Python worker)

### 4. Install and run worker

```bash
cd worker
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
python main.py
```

### 5. Test the flow

1. Login as `patient@healthapp.com` / `Password123!`
2. Book an appointment in the frontend
3. Worker logs a mock email and sets `processingStatus` → `NOTIFIED`

## Configuration

See `.env.example` for all options.

| Variable | Default | Description |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker |
| `H2_HOST` | `localhost` | H2 TCP host |
| `H2_PORT` | `9093` | H2 AUTO_SERVER port (not Kafka's 9092) |
| `H2_DB_PATH` | `./data/appointments` | Same path as backend H2 file |
| `H2_USER` / `H2_PASSWORD` | `sa` / *(empty)* | H2 credentials |
| `NOTIFICATION_MODE` | `mock` | `mock` (console) or `smtp` |

## Tests

```bash
pip install -r requirements.txt
pytest
```

## Project layout

| File | Role |
|---|---|
| `main.py` | Entrypoint |
| `consumer.py` | Kafka consumer loop |
| `handlers.py` | Event routing and business logic |
| `db.py` | H2 JDBC repository |
| `notifications.py` | Email delivery |
| `config.py` | Environment configuration |
