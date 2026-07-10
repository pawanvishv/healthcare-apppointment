# Healthcare Appointment Backend – Project Guide

Quick reference for running, accessing, and testing the Spring Boot backend.

For architecture and design decisions, see [README.md](./README.md).

---

## 1. Project Overview

| Item | Detail |
|---|---|
| **Name** | Healthcare Appointment Backend |
| **Tech** | Java 21, Spring Boot 3.3.6, Maven |
| **Database** | H2 (file mode – data persists across restarts) |
| **Auth** | JWT (access + refresh tokens) |
| **API base path** | `/api/v1` |
| **Default port** | `8080` |

---

## 2. Prerequisites

- **Java 21** (JDK)
- **Maven 3.9+**
- Optional: **Docker** (for containerized run)

Verify installation:

```bash
java -version
mvn -version
```

---

## 3. How to Run

### Option A – Local development (recommended)

```bash
cd backend
mvn spring-boot:run
```

Or build and run the JAR:

```bash
cd backend
mvn clean package -DskipTests
java -jar target/healthcare-appointment-backend-1.0.0-SNAPSHOT.jar
```

### Option B – Docker

```bash
cd backend
docker build -t healthcare-backend .
docker run -p 8080:8080 healthcare-backend
```

### Option C – Custom profile

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

| Profile | Purpose |
|---|---|
| `dev` (default) | H2 file DB, H2 console enabled, debug logging, Kafka off |
| `docker` | Container deployment, Kafka can be enabled via env |

---

## 4. Access URLs

Once the app is running:

| Resource | URL |
|---|---|
| **API base** | http://localhost:8080/api/v1 |
| **Swagger UI** (test APIs) | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs |
| **Health check** | http://localhost:8080/actuator/health |
| **H2 Console** (dev only) | http://localhost:8080/h2-console |

### H2 Console connection (dev profile)

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/appointments` |
| Username | `sa` |
| Password | *(leave empty)* |

---

## 5. Test Credentials (Seed Data)

These users are created automatically on first startup via Flyway migrations.

| Role | Email | Password |
|---|---|---|
| **Patient** | `patient@healthapp.com` | `Password123!` |
| **Doctor** | `doctor@healthapp.com` | `Password123!` |

> Passwords are hashed with BCrypt on startup if the seed hash is a placeholder.

### Seed IDs for API testing

| Entity | ID |
|---|---|
| Doctor profile | `d-101` |
| Patient profile | `p-201` |
| Sample slots | `s-001` … `s-012` |
| Slot dates | `2026-07-10`, `2026-07-11` |

---

## 6. API Endpoints

### Public (no token required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login, get tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| GET | `/actuator/health` | Health check |

### Protected (JWT required)

| Method | Endpoint | Who can use |
|---|---|---|
| POST | `/api/v1/auth/logout` | Any authenticated user |
| GET | `/api/v1/slots?doctorId=&date=` | Any authenticated user |
| POST | `/api/v1/appointments` | **PATIENT only** |
| GET | `/api/v1/appointments/me` | Authenticated patient |
| GET | `/api/v1/appointments/{id}` | Owner, DOCTOR, or ADMIN |
| PATCH | `/api/v1/appointments/{id}/cancel` | Owner or ADMIN |
| PATCH | `/api/v1/appointments/{id}/reschedule` | Owner (patient) |
| GET | `/api/v1/appointments/{id}/history` | Owner, DOCTOR, or ADMIN |

---

## 7. Testing with Swagger UI

### Step 1 – Login

1. Open http://localhost:8080/swagger-ui.html
2. Go to **Authentication** → `POST /api/v1/auth/login`
3. Click **Try it out** and use:

```json
{
  "email": "patient@healthapp.com",
  "password": "Password123!"
}
```

4. Click **Execute**
5. Copy `data.accessToken` from the response

### Step 2 – Authorize

1. Click the green **Authorize** button (top right)
2. Paste the **access token only** (do not add `Bearer`)
3. Click **Authorize** → **Close**

### Step 3 – Test appointment flow

**Get slots**

- `GET /api/v1/slots`
- `doctorId`: `d-101`
- `date`: `2026-07-10`

**Book appointment** (must be logged in as patient)

- `POST /api/v1/appointments`

```json
{
  "doctorId": "d-101",
  "slotId": "s-001"
}
```

**List my appointments**

- `GET /api/v1/appointments/me`
- `page`: `0`, `size`: `10`

**Get appointment details**

- `GET /api/v1/appointments/{id}` — use the `id` from the booking response

**Cancel appointment**

- `PATCH /api/v1/appointments/{id}/cancel`

**Reschedule appointment**

- `PATCH /api/v1/appointments/{id}/reschedule`

```json
{
  "newSlotId": "s-002"
}
```

---

## 8. Testing with cURL / Postman

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"patient@healthapp.com\",\"password\":\"Password123!\"}"
```

Save the `accessToken` from the response.

### Get slots

```bash
curl "http://localhost:8080/api/v1/slots?doctorId=d-101&date=2026-07-10" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Book appointment

```bash
curl -X POST http://localhost:8080/api/v1/appointments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Idempotency-Key: book-test-001" \
  -d "{\"doctorId\":\"d-101\",\"slotId\":\"s-001\"}"
```

### Register a new patient

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"newpatient@example.com\",\"password\":\"Password123!\",\"role\":\"PATIENT\",\"phone\":\"+1234567890\"}"
```

### Register a new doctor

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"newdoctor@example.com\",\"password\":\"Password123!\",\"role\":\"DOCTOR\",\"specialization\":\"Cardiology\"}"
```

---

## 9. Response Format

All API responses use a consistent envelope:

**Success**

```json
{
  "success": true,
  "data": { },
  "error": null
}
```

**Error**

```json
{
  "success": false,
  "data": null,
  "error": {
    "timestamp": "2026-07-10T09:00:00Z",
    "path": "/api/v1/appointments",
    "status": 409,
    "errorCode": "SLOT_UNAVAILABLE",
    "message": "Slot no longer available",
    "traceId": "uuid-here",
    "details": null
  }
}
```

---

## 10. Common HTTP Status Codes

| Code | Meaning | Example |
|---|---|---|
| 200 | OK | Get slots, get appointments |
| 201 | Created | Book appointment |
| 400 | Bad request | Invalid input |
| 401 | Unauthorized | Missing or expired token |
| 403 | Forbidden | Wrong role (e.g. doctor booking as patient) |
| 404 | Not found | Doctor/slot/appointment not found |
| 409 | Conflict | Slot already booked |
| 422 | Business rule violation | Cancel within 2-hour window |

---

## 11. Token Details

| Token | Lifetime | Notes |
|---|---|---|
| **Access token** | 15 minutes | Send as `Authorization: Bearer <token>` |
| **Refresh token** | 7 days | Use `POST /api/v1/auth/refresh` when access token expires |

Refresh request body:

```json
{
  "refreshToken": "YOUR_REFRESH_TOKEN"
}
```

---

## 12. Environment Variables

Copy `.env.example` and set values as needed:

```bash
cp .env.example .env
```

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `JWT_SECRET` | *(dev default)* | **Change in production** |
| `JWT_ACCESS_EXPIRATION_MS` | `900000` | 15 min |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | 7 days |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Frontend origin |
| `KAFKA_ENABLED` | `false` | Enable Kafka event publishing |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker |
| `CANCELLATION_WINDOW_HOURS` | `2` | Min hours before slot to cancel |
| `DB_USERNAME` | `sa` | H2 username |
| `DB_PASSWORD` | *(empty)* | H2 password |

---

## 13. Project Structure (short)

```
backend/
├── src/main/java/com/healthapp/backend/
│   ├── domain/           # Business entities & rules
│   ├── application/      # Use cases, DTOs, services
│   ├── adapter/          # REST controllers, JPA, messaging
│   ├── config/           # Security, Swagger, schedulers
│   └── security/         # JWT provider
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/migration/     # Flyway SQL scripts
├── pom.xml
├── Dockerfile
├── .env.example
├── README.md             # Architecture guide
└── PROJECT_GUIDE.md      # This file
```

---

## 14. Build & Test Commands

```bash
# Compile
mvn clean compile

# Run all tests (includes full API integration test suite)
mvn test

# Full build with tests
mvn clean package

# Run app
mvn spring-boot:run
```

### API integration tests

File: `src/test/java/com/healthapp/backend/integration/ApiEndpointIntegrationTest.java`

This test class covers **all API endpoints** in order:

| # | Test | Endpoint |
|---|---|---|
| 1 | Health check | `GET /actuator/health` |
| 2 | Patient login | `POST /api/v1/auth/login` |
| 3 | Invalid login (401) | `POST /api/v1/auth/login` |
| 4 | Register patient | `POST /api/v1/auth/register` |
| 5 | Doctor login | `POST /api/v1/auth/login` |
| 6 | Refresh token | `POST /api/v1/auth/refresh` |
| 7 | Slots without auth (403) | `GET /api/v1/slots` |
| 8 | Slots with auth | `GET /api/v1/slots` |
| 9 | Book as doctor (403) | `POST /api/v1/appointments` |
| 10 | Book without auth (403) | `POST /api/v1/appointments` |
| 11 | Book as patient (201) | `POST /api/v1/appointments` |
| 12 | Idempotency key | `POST /api/v1/appointments` |
| 13 | Double-book (409) | `POST /api/v1/appointments` |
| 14 | My appointments | `GET /api/v1/appointments/me` |
| 15 | Get by ID | `GET /api/v1/appointments/{id}` |
| 16 | History | `GET /api/v1/appointments/{id}/history` |
| 17 | Reschedule | `PATCH /api/v1/appointments/{id}/reschedule` |
| 18 | Cancel | `PATCH /api/v1/appointments/{id}/cancel` |
| 19 | Logout | `POST /api/v1/auth/logout` |

Tests use an in-memory H2 database (`test` profile) — separate from dev data.

Run only integration tests:

```bash
mvn test -Dtest=ApiEndpointIntegrationTest
```

---

## 15. Troubleshooting

| Problem | Solution |
|---|---|
| Port 8080 already in use | Stop other process or set `SERVER_PORT=8081` |
| 401 on protected APIs | Login again and re-authorize in Swagger |
| 403 on `POST /appointments` | Login as **patient**, not doctor |
| 409 Slot unavailable | Pick another slot (`s-002`, `s-003`, …) |
| H2 console won't connect | Use JDBC URL `jdbc:h2:file:./data/appointments` from `backend/` directory |
| Token expired | Call `/api/v1/auth/refresh` or login again |
| Build fails | Ensure Java 21 and Maven 3.9+ are installed |

---

## 16. Frontend Integration

When connecting the frontend app:

| Setting | Value |
|---|---|
| API base URL | `http://localhost:8080/api/v1` |
| Auth header | `Authorization: Bearer <accessToken>` |
| CORS origin | `http://localhost:3000` (configured in backend) |
| Login endpoint | `POST /api/v1/auth/login` |
| Token refresh | `POST /api/v1/auth/refresh` |

---

## 17. Quick Start Checklist

- [ ] Java 21 and Maven installed
- [ ] `cd backend && mvn spring-boot:run`
- [ ] Open http://localhost:8080/swagger-ui.html
- [ ] Login with `patient@healthapp.com` / `Password123!`
- [ ] Authorize with the access token
- [ ] Test `GET /slots` with `doctorId=d-101`, `date=2026-07-10`
- [ ] Test `POST /appointments` with `doctorId=d-101`, `slotId=s-001`
- [ ] Test `GET /appointments/me`

---

*Last updated: July 2026*
