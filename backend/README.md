# Backend – Healthcare Appointment Platform (Spring Boot)

This document is an **architecture and implementation guide** for the Spring Boot backend of the healthcare appointment platform. It does not contain full source code — it describes structure, decisions, and the reasoning a developer needs to implement the service end‑to‑end.

The backend is one of two collaborating services:

- **`backend/`** – Spring Boot (Java) – REST API, business logic, persistence, event publishing, JWT auth.
- **A Python worker** (documented separately, e.g. `worker/README.md`) – consumes events from the message broker, sends notifications, and reports processing status back.

---

## 1. Architecture Style

We use **Hexagonal Architecture (Ports & Adapters)**, layered so business logic never depends on frameworks, transport, or persistence details.

```
                       ┌─────────────────────────┐
                       │   Inbound Adapters       │
                       │  (REST Controllers,      │
                       │   Kafka/Rabbit listeners)│
                       └────────────┬─────────────┘
                                    │  calls
                       ┌────────────▼─────────────┐
                       │   Application Layer       │
                       │  (Use Cases / Services,   │
                       │   Ports - interfaces)     │
                       └────────────┬─────────────┘
                                    │  calls
                       ┌────────────▼─────────────┐
                       │       Domain Layer         │
                       │ (Entities, Value Objects,  │
                       │  Domain Rules, Exceptions) │
                       └────────────┬─────────────┘
                                    │  implemented by
                       ┌────────────▼─────────────┐
                       │   Outbound Adapters        │
                       │ (JPA Repos, Kafka Producer,│
                       │  Redis Cache, Mail Client) │
                       └─────────────────────────┘
```

**Why Hexagonal/Clean over layered MVC:**
- Domain logic (slot availability, double‑booking prevention, cancellation rules) is testable without Spring context.
- Swapping H2 → Postgres, or Kafka → RabbitMQ, only touches adapters.
- Encourages explicit **ports** (interfaces) such as `AppointmentRepositoryPort`, `EventPublisherPort`, `NotificationPort`.

Dependency rule: **outer layers depend on inner layers, never the reverse.** Domain has zero Spring/JPA/Kafka imports.

---

## 2. Package Structure

```
com.healthapp.backend
│
├── domain/                         # Pure business logic, framework-agnostic
│   ├── model/                      # Appointment, Slot, Patient, Doctor, User (entities/VOs)
│   ├── exception/                  # DomainException, SlotAlreadyBookedException, etc.
│   └── service/                    # Domain services: SlotAvailabilityPolicy, CancellationPolicy
│
├── application/                    # Use-case orchestration
│   ├── port/
│   │   ├── in/                     # Use-case interfaces: BookAppointmentUseCase, CancelAppointmentUseCase
│   │   └── out/                    # Repository/publisher interfaces: AppointmentRepositoryPort, EventPublisherPort
│   ├── service/                    # Use-case implementations (AppointmentService, AuthService)
│   └── dto/                        # Request/Response DTOs, mappers (MapStruct)
│
├── adapter/
│   ├── in/
│   │   ├── web/                    # REST controllers, request validators, exception mappers
│   │   │   ├── controller/
│   │   │   ├── advice/             # @RestControllerAdvice - GlobalExceptionHandler
│   │   │   └── filter/             # JwtAuthenticationFilter
│   │   └── messaging/              # (optional) inbound listeners e.g. status-update consumer
│   └── out/
│       ├── persistence/            # JPA entities, Spring Data repositories, adapters implementing out ports
│       │   ├── entity/
│       │   ├── repository/
│       │   └── mapper/             # domain <-> JPA entity mappers
│       ├── messaging/              # KafkaEventPublisher / RabbitEventPublisher (implements EventPublisherPort)
│       └── cache/                  # RedisSlotCacheAdapter / CaffeineSlotCacheAdapter
│
├── config/                         # SecurityConfig, KafkaConfig, SwaggerConfig, CacheConfig, SchedulerConfig
├── security/                       # JwtTokenProvider, UserDetailsServiceImpl, RoleConstants
└── BackendApplication.java
```

**Purpose summary**

| Package | Responsibility |
|---|---|
| `domain` | Entities and rules with zero framework dependency; the "truth" of the business |
| `application.port.in` | What the system can do (use cases) |
| `application.port.out` | What the system needs from the outside world |
| `application.service` | Orchestrates domain + ports to fulfill a use case, owns transactions |
| `adapter.in.web` | Translates HTTP ⇄ application layer |
| `adapter.out.persistence` | Translates JPA ⇄ domain |
| `adapter.out.messaging` | Publishes domain events to Kafka/RabbitMQ |
| `config` / `security` | Cross-cutting framework wiring |

---

## 3. Technology Stack

| Concern | Choice | Notes |
|---|---|---|
| Language / Runtime | Java 21 (LTS) | Virtual threads optional for I/O-bound handlers |
| Framework | Spring Boot 3.3.x | Spring Web, Spring Security, Spring Data JPA, Spring Validation |
| Database | H2 (file mode for persistence across restarts) | Swappable to Postgres via `spring.datasource` profile |
| Messaging | Kafka (primary) with RabbitMQ as documented alternative | Spring Kafka |
| Auth | JWT (access + refresh tokens) | `jjwt` or `nimbus-jose-jwt` |
| API Docs | springdoc-openapi (Swagger UI) | Auto-generated from annotated controllers |
| Migrations | Flyway | Versioned schema, works with H2 and Postgres alike |
| Caching | Caffeine (in-memory) or Redis (distributed) | For available-slots read path |
| Object Mapping | MapStruct | Compile-time DTO/entity/domain mapping |
| Build | Maven (or Gradle) | Multi-profile: `dev`, `test`, `docker` |
| Containerization | Docker, multi-stage build | Distroless/JRE-slim final image |
| Testing | JUnit 5, Mockito, Testcontainers, Rest-Assured | See §17 |
| Static Analysis | SonarQube / SonarLint | Quality gate in CI |
| Coverage | JaCoCo | Threshold enforced in build |

---

## 4. REST API Design

Base path: `/api/v1`

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/auth/register` | Register new user (PATIENT or DOCTOR role) | Public |
| POST | `/auth/login` | Authenticate, returns access + refresh token | Public |
| POST | `/auth/refresh` | Exchange refresh token for new access token | Public (valid refresh token) |
| POST | `/auth/logout` | Revoke refresh token | Authenticated |
| GET | `/slots?doctorId=&date=` | Fetch available slots for a doctor/date | Authenticated |
| POST | `/appointments` | Create/book an appointment | Authenticated (PATIENT) |
| GET | `/appointments/me` | Fetch current user's appointments (paginated, filterable by status) | Authenticated |
| GET | `/appointments/{id}` | Fetch single appointment detail | Authenticated (owner or DOCTOR/ADMIN) |
| PATCH | `/appointments/{id}/cancel` | Cancel an appointment | Authenticated (owner or ADMIN) |
| PATCH | `/appointments/{id}/reschedule` | Reschedule to a new slot | Authenticated (owner) |
| GET | `/appointments/{id}/history` | Fetch audit/status-log trail for an appointment | Authenticated |
| GET | `/actuator/health` | Liveness/readiness | Public/internal |

**Conventions**
- Versioned via URI prefix (`/api/v1`) so future breaking changes don't disrupt existing clients.
- Pagination: `page`, `size`, `sort` query params on list endpoints; response wrapped as `PageResponse<T>` (`content`, `totalElements`, `totalPages`, `page`).
- All responses use a consistent envelope: `{ "success": boolean, "data": ..., "error": null }` for uniform frontend handling, or standard HTTP codes with an `ApiError` body (`timestamp`, `path`, `status`, `code`, `message`, `details[]`) — pick one and apply consistently.
- Idempotency: `POST /appointments` accepts an optional `Idempotency-Key` header to safely retry on network failure without double-booking.
- `GET /appointments/{id}` and `GET /appointments/me` responses include a `processingStatus` field (`PENDING` / `NOTIFIED` / `FAILED`) sourced from `notification_status` — this is the single field the frontend polls to render "Processing notification event..." → "Notified" (see §6, step 5). No separate status endpoint is needed.

**Illustrative payloads** (shapes only, not the full contract):

```
POST /appointments
{ "doctorId": "d-101", "slotId": "s-4521" }

201 Created
{
  "id": "a-9001", "status": "CONFIRMED", "processingStatus": "PENDING",
  "doctorId": "d-101", "slotId": "s-4521", "startTime": "2026-07-10T09:30:00Z"
}

409 Conflict (slot taken)
{ "timestamp": "...", "status": 409, "errorCode": "SLOT_UNAVAILABLE",
  "message": "Slot no longer available", "traceId": "..." }
```

---

## 5. Authentication & Authorization

**Scheme:** JWT access token (short-lived, 15 min) + refresh token (long-lived, 7 days, rotated on use).

- On login: server issues `accessToken` (JWT, signed HS256/RS256, contains `sub`, `roles`, `exp`) and `refreshToken` (opaque random string, stored hashed in DB with `expiresAt`, `revoked` flag, tied to a device/session).
- `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) validates the `Authorization: Bearer` header on every request, populates `SecurityContext`.
- Refresh flow: client calls `/auth/refresh` with the refresh token; server validates it against the stored hash, rotates it (issue new refresh token, revoke old one — refresh token reuse detection guards against theft), and issues a new access token.
- Passwords hashed with **BCrypt** (strength 12).
- **Role-based access control (RBAC):** roles `PATIENT`, `DOCTOR`, `ADMIN` stored on the user; enforced via `@PreAuthorize("hasRole('PATIENT')")` at the use-case/controller boundary, plus resource-ownership checks in the service layer (a patient may only cancel their own appointment unless `ADMIN`).
- CSRF disabled (stateless JWT API), CORS explicitly configured for the frontend origin only.

---

## 6. Appointment Booking Workflow

1. Client requests `GET /slots?doctorId=X&date=Y` → service reads from cache (fallback to DB), returns free slots.
2. Client submits `POST /appointments` with `doctorId`, `slotId`, `patientId` (from JWT), `Idempotency-Key`.
3. `BookAppointmentUseCase`:
   a. Validates request (slot in future, doctor exists, patient not already booked in overlapping slot).
   b. Attempts to reserve the slot **atomically** (see §8, concurrency).
   c. Persists `Appointment` with status `CONFIRMED`, writes an initial `AppointmentLog` entry (`CREATED`).
   d. Publishes `AppointmentBookedEvent` to Kafka topic `appointment.booked` (outbox pattern, §6a).
   e. Returns `201 Created` with appointment details; UI shows "Booking appointment..." → "Appointment booked successfully".
4. Python worker consumes `appointment.booked`, sends notification (email/SMS/mock), then publishes `appointment.notification.processed` (or writes directly to a shared status table).
5. Spring Boot exposes appointment `processingStatus` (`PENDING` → `NOTIFIED` → `FAILED`) so the UI can poll or use SSE/WebSocket for "Processing notification event...".

**6a. Reliable event publishing — Transactional Outbox**
To avoid the dual-write problem (DB commit succeeds but Kafka publish fails, or vice versa):
- Write the `Appointment` row and an `OutboxEvent` row (`aggregateId`, `eventType`, `payload`, `status=PENDING`) in the **same DB transaction**.
- A scheduled poller (or Debezium CDC in production) reads `PENDING` outbox rows and publishes to Kafka, marking them `SENT` on broker ack.
- Guarantees at-least-once delivery; consumer side must be idempotent (dedupe by event ID).
- **Event schema versioning:** every payload carries a `schemaVersion` field (e.g. `"schemaVersion": 1`) from day one, even though only one version exists initially. This costs nothing now and avoids a painful migration later when the Python worker and Spring Boot need to evolve event shapes independently.

```
Client → Controller → BookAppointmentUseCase
                          │
                          ├─ SlotRepository.reserve() [pessimistic/optimistic lock]
                          ├─ AppointmentRepository.save()
                          ├─ OutboxRepository.save(AppointmentBookedEvent)
                          └─ commit (single transaction)
                                   │
                         OutboxPoller (scheduled, every 2s)
                                   │
                             KafkaProducer.send("appointment.booked")
                                   │
                          Python worker consumes → notify → update status
```

---

## 7. Cancellation & Rescheduling Flow

**Cancellation**
- `PATCH /appointments/{id}/cancel`: validate ownership, validate current status is `CONFIRMED` (not already `CANCELLED`/`COMPLETED`), validate cancellation window (e.g. must be >2 hours before slot start — configurable business rule).
- Transition status → `CANCELLED`, release the slot (mark `Slot.available = true`), write `AppointmentLog` entry (`CANCELLED`, reason, timestamp, actor).
- Publish `AppointmentCancelledEvent` (outbox) so the worker can send a cancellation notification.

**Rescheduling**
- `PATCH /appointments/{id}/reschedule` with `newSlotId`. Modeled as an atomic **cancel-old + book-new** inside one transaction:
  1. Validate new slot availability and lock it.
  2. Release old slot.
  3. Update appointment's `slotId`, reset status to `CONFIRMED`, append `AppointmentLog` (`RESCHEDULED`, oldSlot → newSlot).
  4. Publish `AppointmentRescheduledEvent`.
- If new-slot reservation fails (already taken), the whole transaction rolls back — old slot remains booked, client gets `409 Conflict`.

**Status lifecycle**

```
PENDING → CONFIRMED → COMPLETED
             │  ↑
             ▼  │(reschedule = release+rebook)
         CANCELLED
```

---

## 8. Concurrency Handling (Preventing Double Booking)

Two complementary mechanisms:

1. **Single source of truth for availability lives on `slots`, not on `appointments`** (see §9 for the full reasoning): `slots.available` + `slots.version` is the only field a booking attempt atomically flips, guarded by `UNIQUE (doctor_id, start_time)` on the `slots` table itself. `appointments.slot_id` is a plain FK, **not** uniquely constrained — if it were, a cancelled appointment's old row would permanently block that slot from ever being booked again by anyone else. The DB-level guard against double-booking is therefore the conditional update in point 2 below plus the slot table's own uniqueness, not a constraint on the appointments table.

2. **Optimistic locking** on the `Slot` entity via `@Version`:
   - Read slot → check `available == true` → attempt `UPDATE slots SET available = false, version = version+1 WHERE id = ? AND version = ? AND available = true`.
   - If 0 rows updated → `OptimisticLockException` → mapped to `409 Conflict` ("Slot no longer available") → application retries showing next available slot or asks user to pick another.
   - Preferred over pessimistic locking for read-heavy, low-contention slot tables; avoids holding DB locks across the whole request.

3. **Pessimistic locking alternative** (documented for high-contention doctors, e.g. very popular slots):
   `SELECT ... FOR UPDATE` inside a short transaction when reserving a slot, released immediately after commit. Trade-off: reduces throughput under contention but eliminates optimistic-lock retry storms.

4. **Idempotency key** on `POST /appointments` prevents duplicate bookings caused by client retries (network timeout + resubmit) — server caches the response for a given key for a TTL (e.g. 24h) and returns the original result instead of re-processing.

5. Under load, `@Transactional(isolation = Isolation.READ_COMMITTED)` (H2/Postgres default) combined with the unique constraint + optimistic version check is sufficient; `SERIALIZABLE` is avoided for performance reasons.

---

## 9. Database Design (H2)

**Core tables**

- `users (id, email UNIQUE, password_hash, role, created_at, updated_at)`
- `refresh_tokens (id, user_id FK, token_hash, expires_at, revoked, created_at)`
- `doctors (id, user_id FK, specialization, ...)`
- `patients (id, user_id FK, ...)`
- `slots (id, doctor_id FK, start_time, end_time, available BOOLEAN, version INT)`
- `appointments (id, patient_id FK, doctor_id FK, slot_id FK UNIQUE, status, created_at, updated_at)`
- `appointment_logs (id, appointment_id FK, event_type, old_status, new_status, actor, message, created_at)`
- `outbox_events (id, aggregate_id, event_type, payload JSON, status, created_at, sent_at)`
- `notification_status (id, appointment_id FK, channel, status, attempted_at, processed_at)` — updated by the Python worker (or synced via an inbound event)

**Constraints**
- `slots`: `UNIQUE (doctor_id, start_time)` — the actual anti-double-booking guard (see §8, point 1). One row per doctor per time slot, period.
- `appointments`: `slot_id` is **not** uniquely constrained. Multiple appointment rows may reference the same `slot_id` over time (one `CONFIRMED`, any number of earlier `CANCELLED` ones from prior cancel/reschedule cycles) — this is intentional and preserves full history without special-casing deletes.
- A partial/conditional unique index (`CREATE UNIQUE INDEX ... WHERE status = 'CONFIRMED'`, supported on Postgres) is an optional extra safety net once migrated off H2; not required for correctness since `slots.available` + `@Version` already prevents concurrent double-booking.
- Foreign keys with `ON DELETE RESTRICT` for auditability (never hard-delete appointments; use status).

**Entity Relationship overview**

```
User (1) ── (1) Patient        User (1) ── (1) Doctor
                │                                │
                │ (1)                        (1) │
                ▼                                ▼
           Appointment (N) ────────────────── Slot (1)
                │
                │ (1) ── (N)
                ▼
         AppointmentLog

Appointment (1) ── (1) NotificationStatus
```

**H2 configuration note:** run H2 in file-based persistence mode (`jdbc:h2:file:./data/appointments`) rather than in-memory, so data survives restarts during development; enable H2 console only in `dev` profile. Flyway migrations (`V1__init_schema.sql`, `V2__seed_slots.sql`, …) keep schema evolution explicit and portable to Postgres.

---

## 10. Validation Strategy

- **Bean Validation (JSR-380)** annotations on DTOs: `@NotBlank`, `@Email`, `@Future` (slot time), `@Pattern` (phone), custom `@ValidRole`.
- Layered validation:
  - **Syntactic** (DTO annotations) — caught at controller boundary via `@Valid`, produces `400 Bad Request` with field-level errors.
  - **Semantic/business** (domain rules) — e.g. "cannot cancel within 2 hours of appointment", "slot must belong to requested doctor" — enforced in the application/domain layer, throws typed `DomainException` subclasses, mapped to `409/422`.
- Custom validators as `ConstraintValidator` implementations for reusable rules (e.g. `@FutureSlot`).
- Never trust client-supplied `patientId`/`role` — always derive identity from the authenticated JWT principal.

---

## 11. Global Exception Handling

Centralized via `@RestControllerAdvice`:

| Exception | HTTP Status | Response |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Field-level validation errors |
| `AuthenticationException` / bad credentials | 401 | Generic "invalid credentials" (no user enumeration) |
| `AccessDeniedException` | 403 | "Not authorized" |
| `ResourceNotFoundException` | 404 | e.g. appointment/slot not found |
| `SlotAlreadyBookedException` / `OptimisticLockException` | 409 | "Slot no longer available" |
| `IllegalStateTransitionException` (e.g. cancel a completed appt) | 422 | Business rule violation |
| `Exception` (fallback) | 500 | Generic message; full stack trace only in logs, never in response |

All error responses share a consistent `ApiError` shape (`timestamp`, `path`, `status`, `errorCode`, `message`, `details[]`) and include a `traceId` (correlation ID) to cross-reference logs.

---

## 12. Logging Strategy

- **SLF4J + Logback**, structured JSON output in non-dev profiles (for log aggregation, e.g. ELK/CloudWatch).
- **Correlation/trace ID**: generated (or propagated from an inbound header) per request via a `Filter`, stored in **MDC**, included in every log line and forwarded in outbound event payloads/headers so the Python worker's logs can be correlated to the same request.
- Log levels: `INFO` for business events (appointment booked/cancelled), `WARN` for recoverable issues (optimistic lock retry), `ERROR` for unhandled exceptions, `DEBUG` for SQL/detailed flow (dev only).
- Never log PII (raw passwords, full JWTs, patient personal data) — mask/redact in a `Logback` pattern or custom converter.
- Audit trail is **not** logs alone — business-critical state changes are persisted in `appointment_logs` (queryable, durable), logs are for operational diagnostics.

---

## 13. Security Configuration

- `SecurityFilterChain` bean: stateless session (`SessionCreationPolicy.STATELESS`), CSRF disabled, CORS restricted to the frontend origin(s) from config.
- `JwtAuthenticationFilter` placed before `UsernamePasswordAuthenticationFilter`.
- Method-level security enabled (`@EnableMethodSecurity`) for `@PreAuthorize` checks.
- Password encoder: `BCryptPasswordEncoder`.
- Rate limiting on `/auth/login` and `/auth/register` (e.g. Bucket4j) to mitigate brute force.
- Security headers: `Strict-Transport-Security`, `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Content-Security-Policy` for the small server-rendered surfaces (Swagger UI, actuator).
- Secrets (JWT signing key, DB credentials) sourced from environment variables / a secrets manager — never committed.

---

## 14. Caching Strategy

- **Available slots** are read far more often than written → good caching candidate.
- Local dev/small scale: **Caffeine** (`@Cacheable("availableSlots")`, key = `doctorId+date`, TTL ~30s).
- Multi-instance/production: **Redis**, so cache is consistent across horizontally scaled pods; also usable for refresh-token/session lookups and rate-limit counters.
- **Cache invalidation:** on successful booking/cancellation/reschedule, evict the affected `doctorId+date` cache key (`@CacheEvict`) rather than relying purely on TTL, to avoid showing stale "available" slots.
- Read-through pattern: cache miss → query DB → populate cache → return.

---

## 15. Scheduler / Background Jobs

Implemented with Spring's `@Scheduled` (or a dedicated `TaskScheduler` bean; Quartz if jobs need persistence/clustering guarantees):

- **Outbox poller** — every 2s, publishes pending `OutboxEvent`s to Kafka/RabbitMQ.
- **Stale-hold cleanup** — every minute, releases slots that were tentatively reserved (if a "hold before pay/confirm" step exists) but never confirmed within N minutes.
- **Appointment status finalizer** — hourly job marks past `CONFIRMED` appointments as `COMPLETED`.
- **Reminder trigger** — publishes a `ReminderDueEvent` ~24h before an appointment for the Python worker to notify the patient.
- Jobs run on a single instance in multi-node deployments via a distributed lock (e.g. `ShedLock` backed by the DB) to avoid duplicate execution.

---

## 16. JVM Considerations

- **Heap sizing:** start with `-Xms512m -Xmx512m` for a small service; size based on load-tested working set, avoid huge headroom that just delays GC pauses. Container memory limit should be ~1.3–1.5× `-Xmx` to leave room for metaspace/thread stacks/native memory.
- **Garbage Collector:** default **G1GC** is a good fit for this workload (moderate heap, latency-sensitive REST API). For very low-latency needs at larger heaps, evaluate **ZGC**. Set `-XX:MaxGCPauseMillis=200` as a target, not a guarantee.
- **Thread pools:**
  - Tomcat/Undertow worker pool sized for expected concurrency (`server.tomcat.threads.max`), not left at defaults blindly — tune against DB connection pool size to avoid thread starvation waiting on connections.
  - HikariCP connection pool sized conservatively (`maximumPoolSize` ≈ `(core_count * 2) + effective_spindle_count`, or empirically tuned — typically 10–20 for this workload), since oversized pools cause DB-side contention, not throughput gains.
  - Async/event work (outbox publishing) can use a small dedicated `ExecutorService`/`@Async` pool separate from the request-handling pool, so slow Kafka calls don't starve HTTP threads.
  - Java 21 **virtual threads** (`spring.threads.virtual.enabled=true`) are a strong option for this I/O-bound service (many blocking DB/Kafka calls) to increase concurrency without inflating platform-thread counts — worth a load-test comparison against platform threads.
- **Startup/footprint:** enable Spring Boot's lazy initialization only if startup time matters more than first-request latency; prefer AOT/CDS (`-XX:SharedArchiveFile`) or GraalVM native image for faster cold starts if deployed in serverless/scale-to-zero contexts.
- **Monitoring:** expose JVM metrics via Actuator + Micrometer (heap, GC pause time, thread count, connection pool utilization) to Prometheus/Grafana.

---

## 17. Scalability Considerations

- **Stateless service** (JWT auth, no server-side session) → horizontally scalable behind a load balancer.
- **Database as the contention point** for double-booking prevention: unique constraints + optimistic locking keep DB-level locking short-lived even under many instances.
- **Kafka partitioning:** partition `appointment.*` topics by `doctorId` (or `appointmentId`) to preserve per-aggregate ordering while allowing parallel consumption across partitions.
- **Read/write separation candidate:** slot-availability reads can be served from cache/replica; writes (bookings) go to primary.
- **Backpressure:** consumer-side (Python) should be independently scalable (add consumer instances up to partition count) so notification processing doesn't bottleneck booking throughput.
- **Idempotent consumers** everywhere events are consumed, since Kafka/RabbitMQ guarantee at-least-once delivery, not exactly-once.
- Migration path documented from H2 → Postgres (same JPA/Flyway setup) for real horizontal DB scaling (read replicas, connection pooling via PgBouncer) beyond the assignment's scope.

---

## 18. Testing Strategy

| Layer | Tool | What's covered |
|---|---|---|
| **Unit** | JUnit 5 + Mockito | Domain rules (slot availability, cancellation window), use-case services with mocked ports — no Spring context, fast |
| **Repository** | `@DataJpaTest` + Testcontainers (or H2 in-memory for pure unit speed) | Query correctness, unique constraints, optimistic locking behavior under concurrent updates |
| **Controller** | `@WebMvcTest` + MockMvc, mocked use-case beans | Request validation, status codes, security rules (401/403), response shape |
| **Integration** | `@SpringBootTest` + Testcontainers (Postgres + Kafka) | Full booking flow: HTTP request → DB write → outbox → Kafka message published, verified via an embedded/test consumer |
| **Contract** | Rest-Assured / Spring Cloud Contract (optional) | API docs match actual behavior |
| **Concurrency** | Custom test harness spinning N parallel threads hitting `POST /appointments` for the same slot | Asserts exactly one booking succeeds, others get `409` |
| **Security** | Dedicated tests for JWT expiry, refresh rotation/reuse detection, role enforcement | Auth edge cases |

**Representative test cases to implement**
- Book a slot successfully → status `CONFIRMED`, slot marked unavailable.
- Book an already-booked slot → `409 Conflict`.
- 20 concurrent requests for the same slot → exactly 1 succeeds.
- Cancel within disallowed window → `422`.
- Cancel already-cancelled appointment → `422`/`409` (idempotency of cancel is a deliberate design choice — document which).
- Reschedule to a taken slot → transaction rolls back, original booking intact.
- Expired access token → `401`; refresh token rotation → old refresh token reuse → all sessions revoked (breach detection).
- Malformed request body → `400` with field errors.
- Unauthorized role attempting doctor-only endpoint → `403`.
- Outbox poller publishes and marks event `SENT`; simulated broker failure → event remains `PENDING`, retried next cycle (no data loss).

---

## 19. Code Coverage Goals (JaCoCo)

- Overall line coverage target: **≥ 80%**.
- Domain + application layers (business rules): **≥ 90%** — this is where correctness matters most.
- Adapters (controllers, persistence): **≥ 70%** — thinner logic, but still covered for regression safety.
- JaCoCo Maven/Gradle plugin configured with a build-breaking threshold (`<rule>` in `jacoco-maven-plugin`) enforced in CI; coverage report published as a build artifact and (optionally) surfaced in PRs.
- Exclusions: generated code (MapStruct impls), DTOs with no logic, config classes.

---

## 20. Static Code Analysis (SonarQube)

- SonarQube (or SonarCloud for a public repo) integrated into CI, running on every PR.
- Quality gate: no new blocker/critical issues, no new security hotspots left unreviewed, duplication < 3%, maintainability rating A on new code.
- Checks: cyclomatic complexity, code smells, unused imports, hardcoded secrets detection, SQL injection risk patterns (esp. around any native queries).
- SonarLint recommended locally in the IDE for pre-commit feedback.

---

## 21. API Documentation (OpenAPI/Swagger)

- `springdoc-openapi-starter-webmvc-ui` auto-generates the OpenAPI 3 spec from controller annotations (`@Operation`, `@ApiResponse`, `@Schema`).
- Swagger UI served at `/swagger-ui.html`, raw spec at `/v3/api-docs`.
- JWT bearer auth scheme documented so Swagger UI can be used to obtain and inject a token for "try it out" calls.
- Response DTOs annotated with `@Schema(description = ...)` for meaningful docs, not just field names.
- Exported spec (`openapi.json`) committed/attached as a deliverable per the assignment.

---

## 22. Configuration Management

- Spring profiles: `dev` (H2 console on, verbose logging), `test` (Testcontainers), `docker`/`prod` (env-var-driven, structured logs, H2 file or Postgres).
- `application.yml` (base) + `application-{profile}.yml` overrides.
- Secrets (`JWT_SECRET`, `DB_PASSWORD`, broker credentials) injected via environment variables, never hardcoded; `.env.example` documents required variables without real values.
- Externalized, environment-specific values: Kafka bootstrap servers, cache TTLs, cancellation-window minutes, rate-limit thresholds — all configurable without code changes.

---

## 23. Build & Deployment

- **Build:** Maven (`mvn clean verify`) runs unit + integration tests, JaCoCo, and packages a fat JAR.
- **Docker:** multi-stage `Dockerfile` — build stage (Maven + JDK) produces the JAR, runtime stage copies it onto a slim JRE (e.g. `eclipse-temurin:21-jre-alpine`), runs as non-root user, exposes the app port, sets JVM flags via `JAVA_OPTS` env var.
- **docker-compose.yml** (repo root, outside both `backend/` and `frontend/`) wires together: `backend`, `worker` (Python), `frontend`, `kafka` (+ `zookeeper` or KRaft mode), `postgres` (or bundled H2 file volume), and optionally `redis` — `frontend`'s `NEXT_PUBLIC_API_BASE_URL` points at the `backend` service's compose-network hostname. `docker compose up` gives one command that brings up the entire platform end-to-end — this satisfies the "Dockerized deployment" bonus point.
- **CI pipeline** (GitHub Actions): lint → test → coverage gate → Sonar scan → build image → (optionally) push to a registry.
- **Health checks:** Actuator `/actuator/health` wired into Docker `HEALTHCHECK` and used by compose/orchestrator readiness probes.

---

## 24. Sequence Diagram — End-to-End Booking

```
Patient(UI)   SpringBoot API      DB          Outbox/Kafka        Python Worker
    │              │               │                │                    │
    │ GET /slots   │               │                │                    │
    │─────────────▶│  (cache/DB)   │                │                    │
    │◀─────────────│               │                │                    │
    │ POST /appointments           │                │                    │
    │─────────────▶│               │                │                    │
    │              │ lock+reserve  │                │                    │
    │              │──────────────▶│                │                    │
    │              │ save appt+outbox (1 txn)        │                    │
    │              │──────────────▶│                │                    │
    │◀─────────────│ 201 Created   │                │                    │
    │              │               │  poll outbox   │                    │
    │              │               │───────────────▶│                    │
    │              │               │                │ publish event      │
    │              │               │                │───────────────────▶│
    │              │               │                │                    │ send notification
    │              │               │                │◀───────────────────│ publish status event
    │  poll/SSE status             │                │                    │
    │─────────────▶│◀──────────────────────────────────────────────────│
    │◀─────────────│ NOTIFIED     │                │                    │
```

---

## 25. Project Folder Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/healthapp/backend/
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   ├── adapter/
│   │   │   ├── config/
│   │   │   ├── security/
│   │   │   └── BackendApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-docker.yml
│   │       └── db/migration/        # Flyway V1__..., V2__...
│   └── test/
│       └── java/com/healthapp/backend/
│           ├── unit/
│           ├── integration/
│           └── controller/
├── Dockerfile
├── pom.xml
└── README.md   ← this file
```

---

## 26. Edge Case & Error Scenario Catalog

Consolidated list a developer should explicitly handle and write tests for (individual cases are referenced from earlier sections; this is the single checklist view):

| # | Scenario | Expected Behavior |
|---|---|---|
| 1 | Booking a slot in the past | `400` — rejected at validation (`@Future`), never reaches the domain layer |
| 2 | Two requests booking the same slot simultaneously | Exactly one `201`, the other `409` (§8) |
| 3 | Client retries a booking POST after a timeout (network flake, booking actually succeeded) | Same `Idempotency-Key` returns the original `201` response, no duplicate row |
| 4 | Doctor/slot referenced in request doesn't exist | `404` |
| 5 | Cancelling someone else's appointment | `403` (ownership check, not just 404 — avoids leaking existence to the wrong user via error-code difference, so consider returning 404 instead of 403 to prevent resource enumeration; pick one and document it) |
| 6 | Cancelling within the disallowed cancellation window | `422` with a message stating the cutoff |
| 7 | Cancelling an already-cancelled or completed appointment | `409`/`422` — no-op, not a silent success (state machine rejects invalid transition) |
| 8 | Rescheduling to a slot that gets taken mid-request | Whole transaction rolls back, original booking untouched, client sees `409` |
| 9 | Access token expired mid-session | `401` → frontend silently refreshes and retries once |
| 10 | Refresh token reused after rotation (stolen/replayed) | All sessions for that user revoked, forces re-login (breach containment) |
| 11 | Malformed/missing required fields | `400` with per-field error details |
| 12 | Patient double-books overlapping slots with two different doctors | Allowed unless a business rule says otherwise — **explicitly decide and document this**; default here is "allowed" since patients may legitimately see two providers, but flag it as a configurable policy |
| 13 | Kafka/RabbitMQ broker temporarily unreachable | Booking still succeeds (outbox decouples the write from the publish); event delivered once the broker recovers, no user-facing failure |
| 14 | Python worker fails to send a notification | `processingStatus = FAILED`, visible on the appointment; worker retries per its own policy without blocking the booking itself |
| 15 | Slot deleted/deactivated by an admin while a booking request is in flight | Optimistic-lock/availability check fails the booking with `409` rather than succeeding against a stale slot |
| 16 | Pagination requested beyond the last page | `200` with an empty `content` array, not an error |
| 17 | Clock/timezone mismatch between client and server | All timestamps are UTC on the wire (`Instant`/ISO-8601 with `Z`); the frontend converts to local display time — server never trusts a client-supplied local time as authoritative |

---

## 27. Non-Functional Requirements Summary

| NFR | Approach |
|---|---|
| Availability | Stateless instances behind LB, health-checked, graceful shutdown (drain in-flight requests) |
| Consistency | Strong consistency for booking (transactional + unique constraint), eventual consistency for notification status |
| Latency | Cached slot reads (<50ms typical), booking write path optimized to a single short transaction |
| Security | JWT + RBAC, BCrypt, rate limiting, no PII in logs, HTTPS-terminated at ingress |
| Observability | Structured logs with correlation IDs, Actuator/Micrometer metrics, JaCoCo/Sonar quality gates |
| Portability | H2 → Postgres via Flyway with no code changes; Kafka → RabbitMQ isolated to `adapter.out.messaging` |
