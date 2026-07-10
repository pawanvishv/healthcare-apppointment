# Healthcare Appointment Frontend – Project Guide

Quick reference for running and using the Next.js frontend connected to the Spring Boot backend.

For architecture details, see [README.md](./README.md).  
For backend API and credentials, see [../backend/PROJECT_GUIDE.md](../backend/PROJECT_GUIDE.md).

---

## 1. Overview

| Item | Detail |
|---|---|
| **Framework** | Next.js 14 (App Router) + TypeScript |
| **Styling** | Tailwind CSS |
| **State** | TanStack Query (server) + Zustand (auth) |
| **Forms** | React Hook Form + Zod |
| **Default port** | `3000` |
| **Backend API** | `http://localhost:8080/api/v1` |

---

## 2. Prerequisites

- **Node.js 18+**
- **Backend running** on port 8080

```bash
# Terminal 1 – Backend
cd backend
mvn spring-boot:run

# Terminal 2 – Frontend
cd frontend
npm install
npm run dev
```

Open: **http://localhost:3000**

---

## 3. Test Credentials

| Role | Email | Password |
|---|---|---|
| Patient | `patient@healthapp.com` | `Password123!` |
| Doctor | `doctor@healthapp.com` | `Password123!` |

**Seed doctor ID:** `d-101`  
**Sample slot dates:** `2026-07-10`, `2026-07-11`

---

## 4. App Routes

| Route | Description | Auth |
|---|---|---|
| `/login` | Sign in | Public |
| `/register` | Create account | Public |
| `/appointments` | My appointments list | Protected |
| `/appointments/new` | Book appointment | Protected (Patient) |
| `/appointments/[id]` | Detail, cancel, reschedule | Protected |
| `/slots` | Browse available slots | Protected |

---

## 5. Features Implemented

- Login / Register with JWT token storage
- Auto token refresh on 401
- Browse available slots by doctor + date
- Book appointment with idempotency key
- View my appointments (with status filter)
- Appointment detail with live notification status polling
- Cancel and reschedule appointments
- Audit history timeline
- Toast notifications for success/error
- Responsive sidebar (mobile drawer)
- Role guard (only patients can book)

---

## 6. Environment Variables

Copy `.env.example` to `.env.local`:

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_APP_ENV=development
```

---

## 7. Scripts

```bash
npm run dev      # Development server
npm run build    # Production build
npm run start    # Start production server
npm run lint     # ESLint
```

---

## 8. User Flow

```
Login → Browse Slots → Select Slot → Book Appointment
  → View Detail (polls notification status)
  → Cancel or Reschedule
```

**Status messages shown in UI:**
- "Fetching available slots..."
- "Booking appointment..."
- "Appointment booked successfully!"
- "Processing notification..." → "Notified"

---

## 9. Project Structure

```
frontend/
├── app/                  # Next.js routes
│   ├── (auth)/           # login, register
│   └── (dashboard)/      # appointments, slots
├── features/             # auth, appointments, slots
├── components/           # ui + layout
├── lib/                  # http client, env, utils
├── store/                # Zustand auth store
├── types/                # API DTO types
└── PROJECT_GUIDE.md      # This file
```

---

## 10. Troubleshooting

| Problem | Solution |
|---|---|
| API calls fail | Ensure backend is running on port 8080 |
| CORS error | Backend `CORS_ALLOWED_ORIGINS` must include `http://localhost:3000` |
| 403 on booking | Login as **patient**, not doctor |
| Slots empty | Use doctor `d-101` and date `2026-07-10` |
| Token expired | App auto-refreshes; re-login if refresh fails |

---

*Last updated: July 2026*
