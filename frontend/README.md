# Frontend – Healthcare Appointment Platform (Next.js)

This document is an **architecture and implementation guide** for the frontend, which demonstrates the backend workflows described in `backend/README.md`: registration/login, browsing available slots, booking, cancelling/rescheduling appointments, and visualizing event/processing status ("Fetching available slots...", "Booking appointment...", "Appointment booked successfully", "Processing notification event...").

---

## 1. Overall Architecture

**Next.js (App Router) + TypeScript**, structured as a feature-based frontend that talks to the Spring Boot backend exclusively over the documented REST API (`/api/v1/...`).

```
┌─────────────────────────────────────────────┐
│                 Pages/Routes                  │  (App Router: app/)
│   (compose features, handle route params)     │
├─────────────────────────────────────────────┤
│                Feature Modules                │  (features/auth, features/appointments)
│  (components, hooks, feature-local state)      │
├─────────────────────────────────────────────┤
│           Shared UI Components Library         │  (components/ui - buttons, inputs, modals)
├─────────────────────────────────────────────┤
│      API Layer (typed clients + React Query)   │  (lib/api, hooks/queries)
├─────────────────────────────────────────────┤
│         Cross-cutting: auth, error, config      │  (lib/auth, lib/http, lib/env)
└─────────────────────────────────────────────┘
```

Principles:
- **Feature-first organization** — each domain concept (auth, appointments, slots) owns its components, hooks, and types, instead of grouping globally by "components/pages/hooks".
- **Server Components by default**, Client Components (`"use client"`) only where interactivity/state is required (forms, live status widgets).
- **Thin pages** — route files mostly compose feature components; business/display logic lives in the feature modules, keeping routes easy to scan.

---

## 2. Technology Stack

| Concern | Choice | Notes |
|---|---|---|
| Framework | Next.js 14+ (App Router) | SSR for initial data (e.g. slot list), CSR for interactive booking |
| Language | TypeScript (strict mode) | End-to-end type safety with API DTOs |
| Styling | Tailwind CSS | Utility-first, fast iteration, consistent design tokens |
| Server State | TanStack Query (React Query) | Caching, retries, background refetch for slots/appointments |
| Client State | Zustand (small global state: auth/session) + local `useState`/`useReducer` for forms | Avoid over-centralizing state that's naturally local |
| Forms | React Hook Form + Zod | Schema-based validation shared shape with backend DTOs |
| HTTP Client | `fetch` wrapper (or Axios) with interceptors | Centralized auth header injection, refresh-token handling, error normalization |
| Realtime status | Polling (React Query `refetchInterval`) as default; WebSocket/SSE as an upgrade path | Matches backend's "processingStatus" polling model from §6 of backend README |
| Testing | Jest + React Testing Library (unit/component), Playwright (E2E) | |
| Icons | lucide-react | |

---

## 3. Folder Structure

```
frontend/
├── app/
│   ├── (auth)/
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── (dashboard)/
│   │   ├── layout.tsx                # protected layout, sidebar/nav
│   │   ├── appointments/
│   │   │   ├── page.tsx              # list / history
│   │   │   ├── new/page.tsx          # booking flow
│   │   │   └── [id]/page.tsx         # appointment detail, cancel/reschedule
│   │   └── slots/page.tsx            # browse available slots
│   ├── layout.tsx                    # root layout (providers)
│   ├── error.tsx                     # global error boundary
│   ├── loading.tsx                   # global loading UI
│   └── not-found.tsx
│
├── features/
│   ├── auth/
│   │   ├── components/ (LoginForm, RegisterForm)
│   │   ├── hooks/ (useLogin, useRegister, useAuthSession)
│   │   ├── api.ts                    # auth endpoints
│   │   └── types.ts
│   ├── appointments/
│   │   ├── components/ (AppointmentCard, AppointmentList, CancelDialog, RescheduleDialog, StatusBadge)
│   │   ├── hooks/ (useAppointments, useBookAppointment, useCancelAppointment, useAppointmentStatus)
│   │   ├── api.ts
│   │   └── types.ts
│   └── slots/
│       ├── components/ (SlotPicker, SlotGrid)
│       ├── hooks/ (useAvailableSlots)
│       ├── api.ts
│       └── types.ts
│
├── components/
│   ├── ui/                           # Button, Input, Select, Modal, Toast, Spinner, ProgressStepper
│   └── layout/                       # Navbar, Sidebar, ProtectedRoute wrapper
│
├── lib/
│   ├── http/                         # fetch wrapper, interceptors, ApiError type
│   ├── auth/                         # token storage strategy, refresh logic, AuthProvider
│   ├── env.ts                        # typed, validated env access
│   └── utils/                        # date/time formatting, status-color mapping
│
├── hooks/                            # cross-feature hooks (useDebounce, useMediaQuery)
├── store/                            # Zustand stores (session store)
├── types/                            # shared DTO types mirroring backend contracts
├── tests/
│   ├── unit/
│   ├── component/
│   └── e2e/
├── public/
├── .env.example
├── next.config.js
├── tailwind.config.ts
├── tsconfig.json
├── package.json
└── README.md   ← this file
```

---

## 4. Routing Strategy

- **App Router** with route groups: `(auth)` for public login/register pages, `(dashboard)` for authenticated app pages — the group layout is where route protection is enforced.
- File-based routes map directly to user-facing flows: `/appointments`, `/appointments/new`, `/appointments/[id]`, `/slots`.
- Dynamic segment `[id]` for appointment detail/cancel/reschedule, keeping deep-linkable, shareable, bookmarkable URLs.
- `loading.tsx`/`error.tsx` per route segment for granular Suspense-driven loading and error boundaries instead of one global spinner.

---

## 5. Authentication Flow

1. `LoginForm` submits credentials → `POST /auth/login` → receives `accessToken` + `refreshToken`.
2. **Token storage:** `accessToken` kept in memory (Zustand store) for the session; `refreshToken` stored in an **HttpOnly, Secure cookie** set by a Next.js Route Handler acting as a thin BFF proxy for auth endpoints — this avoids exposing the long-lived refresh token to JS (XSS mitigation). If a pure SPA approach is preferred instead, document the trade-off explicitly (refresh token in `httpOnly` cookie is still recommended over `localStorage`).
3. `AuthProvider` (Client Component, wraps the app in root layout) hydrates session state on load by attempting a silent refresh if a refresh cookie exists.
4. The HTTP layer's response interceptor watches for `401`s: on first 401, it calls `/auth/refresh` once, retries the original request with the new access token; on refresh failure, clears session and redirects to `/login`.
5. `Logout` calls `/auth/logout` (revokes refresh token server-side) and clears client state.

---

## 6. Protected Routes

- `(dashboard)/layout.tsx` is a Server Component that checks for a valid session (via a cookie-based check or a fast server-side call) before rendering children; unauthenticated users are redirected to `/login` with a `redirectTo` query param to return them after login.
- A lightweight `ProtectedRoute`/`RequireAuth` client wrapper is also used for role-gated sub-views (e.g. doctor-only screens), reading role from the decoded session and rendering a 403-style fallback otherwise — mirrors backend RBAC so the UI never shows actions the API would reject.
- Middleware (`middleware.ts`) can additionally guard dashboard routes at the edge for a faster redirect before any page code runs.

---

## 7. State Management Approach

- **Server state** (slots, appointments, user profile) → **TanStack Query**: handles caching, deduplication, retries, and background refetching; query keys namespaced per feature (`['slots', doctorId, date]`, `['appointments', 'me', filters]`).
- **Client/UI state** (form inputs, modal open/close, wizard step) → local component state (`useState`/`useReducer`); kept close to where it's used.
- **Global app state** (auth session, current user, feature flags) → small **Zustand** store — deliberately minimal, since most state is either server state (React Query) or local UI state.
- **Cache invalidation on mutation:** booking/cancelling/rescheduling triggers `queryClient.invalidateQueries(['slots', ...])` and `['appointments', ...])` so the UI reflects backend state immediately after a successful mutation — mirrors the backend's own cache-eviction strategy (§14 of backend README).

---

## 8. API Communication Layer

- `lib/http/client.ts`: thin `fetch` wrapper providing:
  - Base URL from `lib/env.ts` (`NEXT_PUBLIC_API_BASE_URL`).
  - Automatic `Authorization: Bearer <accessToken>` header injection.
  - JSON parsing + typed `ApiError` normalization (maps backend's `ApiError` envelope from §11 of backend README into a consistent client-side shape).
  - 401 → single silent-refresh-and-retry (see §5); repeated 401 → force logout.
  - Request correlation: forwards/generates a trace ID header for cross-service log correlation.
- Each feature's `api.ts` exports typed functions (`bookAppointment(payload): Promise<Appointment>`, `getAvailableSlots(params): Promise<Slot[]>`) built on the shared client — components never call `fetch` directly.
- TanStack Query hooks (`useQuery`/`useMutation`) wrap these functions, giving components loading/error/data states without manual `useEffect` plumbing.

---

## 9. Form Validation

- **React Hook Form** for form state/performance (uncontrolled inputs, minimal re-renders).
- **Zod** schemas define validation rules once per form (registration, login, booking, reschedule) and are the single source of truth; `zodResolver` wires them into RHF.
- Zod schemas mirror backend validation constraints (§10 of backend README) so invalid input is caught client-side before a network round trip, while the backend remains the authoritative validator (defense in depth — never trust client-only validation).
- Field-level error messages shown inline; form-level errors (e.g. backend returns `409 slot taken`) surfaced via a toast plus inline context ("This slot was just booked — pick another").

---

## 10. Appointment Booking Flow (UI)

1. **Slots screen** — `SlotPicker` lets the user choose doctor + date → `useAvailableSlots` query fires `GET /slots` → shows a `ProgressStepper`/toast state: **"Fetching available slots..."** while loading, then a `SlotGrid` of results (empty state if none).
2. User selects a slot → navigates to (or opens a modal for) `/appointments/new` with the chosen `doctorId`/`slotId` pre-filled.
3. On submit, `useBookAppointment` mutation fires `POST /appointments` (with a generated `Idempotency-Key` stored for the duration of the attempt) → UI shows **"Booking appointment..."** (disabled submit button + spinner).
4. On success (`201`): toast **"Appointment booked successfully"**, redirect to `/appointments/[id]`, invalidate slots + appointments queries.
5. On conflict (`409`, slot taken): inline error, auto-refetch slots so the user immediately sees the updated availability instead of retrying blindly.
6. Appointment detail page polls `GET /appointments/{id}` (`refetchInterval`, e.g. every 3s, auto-stopped once `processingStatus` reaches a terminal state) — this is the same endpoint used for the initial detail fetch, per backend §4, so no separate status API is required. Rendering goes **"Processing notification event..."** → **"Notified"** as the Python worker completes, via a `StatusBadge` component with distinct colors/icons per state (`PENDING`, `NOTIFIED`, `FAILED`). SSE/WebSocket push is a documented upgrade path (§2) but is *not* assumed to exist on the backend for this scope — polling is the baseline implementation.

---

## 11. Appointment Management Screens

- **`/appointments`** — paginated list with filters (status, date range), each row a `AppointmentCard` (doctor, time, status badge, quick actions).
- **`/appointments/[id]`** — full detail: doctor info, slot time, status, `AppointmentLog`/history timeline (mirrors backend audit trail), action buttons (Cancel, Reschedule) gated by current status and cancellation-window rules mirrored from the backend (button disabled with a tooltip explaining why, if outside the allowed window).
- **Cancel** — confirmation `Dialog` → `useCancelAppointment` mutation → optimistic UI update (mark as "Cancelling..." immediately) with rollback on failure.
- **Reschedule** — opens `SlotPicker` scoped to the same doctor → confirm → mutation → on `409` (new slot taken), dialog stays open with a refreshed slot list instead of closing on failure.

---

## 12. Error Handling

- **Global error boundary** (`app/error.tsx`) catches unhandled render errors, shows a friendly fallback with a retry action.
- **Per-query errors** surfaced via React Query's `error` state, rendered as inline alerts or toasts depending on severity (validation vs system error).
- **Normalized API errors:** the HTTP layer maps backend status codes to user-facing messages (e.g. 409 → "This slot was just taken", 422 → business-rule message from the backend payload, 401 → silent refresh or redirect to login, 500 → generic "Something went wrong, please try again").
- **Network failures** (offline, timeout) distinguished from server errors, with a retry affordance (React Query's built-in retry with exponential backoff for transient failures, capped to avoid hammering the API).
- Toast notification system (`components/ui/Toast`) as the shared surface for both success and error feedback, so the "Appointment booked successfully" / "Slot no longer available" messaging is consistent app-wide.

---

## 13. Loading States

- **Route-level:** `loading.tsx` per segment (skeleton screens, not blank spinners, for perceived performance) while server components fetch initial data.
- **Component-level:** React Query's `isPending`/`isFetching` flags drive skeletons for slot grids/appointment lists and disabled states for buttons mid-mutation.
- **Multi-step async feedback:** a small `ProgressStepper`/status-message component explicitly visualizes the assignment's example flow — "Fetching available slots..." → "Booking appointment..." → "Appointment booked successfully" → "Processing notification event..." — reused for both the booking flow and the post-booking status view.

---

## 14. UI Component Organization

- **`components/ui/`** — pure, reusable, unopinionated primitives (Button, Input, Select, Modal, Toast, Spinner, Badge). No feature/business logic; styled with Tailwind + variant patterns (e.g. `class-variance-authority`) for consistent theming.
- **`features/*/components/`** — feature-specific composite components built from `ui/` primitives plus business logic/hooks (e.g. `AppointmentCard` knows about appointment status enums; a generic `Card` does not).
- **`components/layout/`** — app shell pieces (Navbar, Sidebar, ProtectedRoute) shared across dashboard routes.
- Naming/co-location convention: each component gets its own folder with `Component.tsx`, `Component.test.tsx`, and an `index.ts` barrel export, keeping tests next to implementation.

---

## 15. Responsive Design Strategy

- **Mobile-first Tailwind breakpoints** (`sm`, `md`, `lg`, `xl`): base styles target small screens, larger breakpoints progressively enhance layout (e.g. slot grid: 1 column mobile → 3–4 columns desktop).
- Navigation collapses to a hamburger/drawer (`Sidebar`) below `md`, persistent sidebar above.
- Forms and dialogs use full-screen modals on mobile, centered dialogs on desktop, via a single responsive `Modal` component rather than separate mobile/desktop components.
- Touch targets sized ≥44px on interactive elements (buttons, slot cells) per accessibility guidance; tested at common breakpoints (375px, 768px, 1280px) during development.

---

## 16. Environment Configuration

- `lib/env.ts` validates required env vars at startup (using Zod) so misconfiguration fails fast and loudly rather than surfacing as a mysterious runtime fetch error.
- `.env.example` documents all variables without secrets:
  ```
  NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
  NEXT_PUBLIC_APP_ENV=development
  ```
- `NEXT_PUBLIC_*` prefix reserved strictly for values safe to expose to the browser; anything sensitive (if a BFF/route-handler proxy is used for auth) stays server-only and unprefixed.
- Separate `.env.development` / `.env.production` (or deployment-platform env config) so the same build can point at different backend URLs per environment.

---

## 17. Testing Strategy

| Layer | Tool | What's covered |
|---|---|---|
| **Unit** | Jest | Pure utils (date formatting, status-color mapping, Zod schemas) |
| **Component** | React Testing Library | Forms (validation errors show correctly), `StatusBadge` renders correct state, `SlotGrid` empty/loading/error states |
| **Hook** | RTL `renderHook` + MSW (Mock Service Worker) | `useBookAppointment`, `useAvailableSlots` against a mocked API layer, including error/conflict responses |
| **Integration** | RTL + MSW | Full booking form flow: fill form → submit → see success toast → mocked API called with correct payload |
| **E2E** | Playwright | Real flows against a running backend (or docker-compose stack): register → login → view slots → book → see status update → cancel |
| **Accessibility** | `jest-axe` / Playwright accessibility checks | No critical a11y violations on key screens |

**Representative test cases**
- Login with invalid credentials shows an inline error, does not navigate.
- Booking form disabled while mutation is in flight; re-enabled on error.
- Booking a slot that becomes unavailable mid-flow shows the 409 message and refreshes the slot list.
- Cancel button hidden/disabled outside the allowed cancellation window.
- Session expiry (mocked 401) triggers a silent refresh and the original request succeeds transparently.
- Refresh failure redirects to `/login` and preserves the intended destination via `redirectTo`.
- Responsive layout: sidebar collapses to drawer under `md` breakpoint (Playwright viewport test).

---

## 18. Code Quality & Tooling

- **ESLint** (`eslint-config-next` + `@typescript-eslint`) and **Prettier**, run via `npm run lint` / `lint-staged` on pre-commit (Husky) so style issues never reach CI.
- `tsconfig.json` in `strict` mode; CI fails the build on type errors, not just lint warnings.
- Import ordering/boundaries enforced (e.g. `eslint-plugin-import`) so `features/*` modules can't reach into each other's internals, only through their public `api.ts`/`index.ts`.
- Commit convention (Conventional Commits) recommended for a readable history, not strictly required by the assignment.

---

## 19. Build & Deployment

- **Build:** `next build` produces an optimized production build; `next start` serves it (or deploy to a Node-compatible host/container — no dependency on a specific hosting platform, keeping the assignment's Docker-first deliverable in mind).
- **Dockerfile:** multi-stage — dependencies stage (`npm ci`), build stage (`next build`), slim runtime stage (`node:20-alpine`) running `next start` as a non-root user, only production `node_modules` and the `.next` build output copied in.
- Wired into the repo-root `docker-compose.yml` alongside `backend`, `worker`, `kafka`, and the database (see backend README §23) — `frontend`'s `NEXT_PUBLIC_API_BASE_URL` build/runtime arg points at the `backend` service's compose-network address so `docker compose up` serves a fully working UI against a live backend with zero manual wiring.
- **CI pipeline** (GitHub Actions): install → lint → type-check → unit/component tests → build → (optionally) Playwright E2E against a docker-compose'd stack → build & tag Docker image.

---

## 20. Edge Cases & Error Scenarios (Frontend)

Mirrors the backend's catalog (backend README §26) from the UI's perspective — these are scenarios the frontend must visibly and gracefully handle, not just the API:

| # | Scenario | Expected UI Behavior |
|---|---|---|
| 1 | User double-clicks "Book" | Submit button disabled the instant the mutation starts; same `Idempotency-Key` reused if the click somehow fires twice |
| 2 | Slot list is empty for the chosen doctor/date | Explicit empty state ("No slots available — try another date"), not a blank grid |
| 3 | Booking succeeds on the server but the response is lost (network drop after submit) | On next load/refetch, the appointment shows up via `useAppointments` even though the UI never saw the `201` — no reliance on client-held state as the source of truth |
| 4 | User's access token expires mid-booking-form-fill (long idle time) | Silent refresh on submit; if refresh also fails, form state is preserved in memory and the user is sent to `/login` with a way back (`redirectTo`) rather than losing their in-progress selection |
| 5 | Backend returns a 409 slot conflict during booking | Inline message + slot list auto-refetched so the user picks from currently-real availability, not a stale grid |
| 6 | Backend is unreachable (down, DNS, CORS misconfig) | Distinguished "can't reach server" state, distinct from a 4xx/5xx, with a manual retry action |
| 7 | Cancel/reschedule attempted outside the allowed window | Action buttons disabled with a tooltip explaining why, mirroring backend §7 — never rely on the backend rejection alone as the only signal |
| 8 | Very slow network (booking mutation pending for many seconds) | Loading state persists (no false "stuck" appearance); a timeout/cancel affordance appears after a threshold rather than spinning forever |
| 9 | Timezone display | All times rendered in the viewer's local timezone, converted client-side from the UTC ISO strings the backend sends (backend §17 of that scenario) — never displayed as raw UTC |
| 10 | Role mismatch (a DOCTOR account hits a PATIENT-only screen via direct URL) | Client-side role gate (§6) shows a 403-style fallback immediately, without waiting on an API round trip to fail first |

---

## 21. Project Folder Structure (Summary)

See §3 above for the full tree. Key top-level pieces for quick reference:

```
frontend/
├── app/            # routes (App Router)
├── features/        # domain modules: auth, appointments, slots
├── components/       # shared UI + layout
├── lib/             # http client, auth, env, utils
├── store/            # Zustand global state
├── types/            # shared DTO types
├── tests/            # unit/component/e2e
└── README.md   ← this file
```
