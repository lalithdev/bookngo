#START
# Frontend Technical Architecture Specification

## 1. Purpose and Scope
This document specifies the technical architecture for the BookNGo web frontend application. It defines the architectural patterns, structural conventions, data flows, routing models, state management boundaries, integration contracts, and development standards governing the React/Vite implementation.

The scope of this application encompasses the customer-facing ticket booking portal (movie catalog browsing, showtime discovery, seat selection, atomic hold acquisition, checkout payment, ticket issuance, booking history, and cancellation) as well as operational routing boundaries for administrative and operator portals.

---

## 2. Architectural Principles
1. **Server Authority**: The frontend is strictly a presentation and interaction layer. The backend microservices remain the sole authoritative source of truth for seat availability, pricing, hold lifecycles, booking states, payment outcomes, and authorization.
2. **Zero Optimistic Confirmation**: Under no circumstances does the client optimistically mark seats as held, bookings as confirmed, or payments as successful. State transitions occur only upon receipt of verified server responses.
3. **Single Gateway Boundary**: All network communication flows strictly through the central API Gateway (`http://localhost:8080`). Direct client connections to internal service ports (e.g., User Service `:8081`, Booking Service `:8085`) are prohibited.
4. **Resilience to Stale State**: Given high concurrency during popular movie releases, the client treats all displayed inventory as snapshot data that may change at any millisecond. Contention, conflicts, and expirations are first-class UI states rather than unexpected crashes.
5. **No Second Business Engine**: Business rules (e.g., 5-minute hold limits, 2-minute payment grace windows, cancellation eligibility policies, dynamic seat pricing calculations) are computed exclusively by backend domain services. The frontend only displays server-computed values.
6. **Separation of Concerns**: Strict boundary separation between server cache state (TanStack Query), local interaction state (React hooks), authentication context, and navigation state.

---

## 3. Technology Stack
The frontend application uses a modern, lightweight, non-bloated JavaScript stack:

| Layer | Technology | Specification / Version Focus |
|---|---|---|
| **Build & Runtime** | Vite | Lightning-fast ESM dev server and optimized rollup production bundling |
| **UI Library** | React 19 / 18+ | Functional components, Hooks, Suspense boundaries |
| **Language** | JavaScript (ES2022+) | Native modern JavaScript with standard ES modules; no TypeScript compilation overhead |
| **Styling** | Tailwind CSS v4 | Utility-first CSS framework configured via `@tailwindcss/vite` and modern CSS `@theme` tokens (no PostCSS config) |
| **Routing** | React Router (current) | Declarative client-side routing, nested layouts, route loaders, and protected route wrappers |
| **Server State** | TanStack Query v5+ | Asynchronous server-state synchronization, background refetching, caching, and mutation tracking |
| **HTTP Client** | Fetch API / Axios wrapper | Unified HTTP client configured with interceptors for JWT injection and idempotency |
| **Icons & Media** | Lucide React | Lightweight, accessible SVG icon set |
| **Date Handling** | date-fns | Lightweight, modular date formatting for showtimes and countdowns |

*Note: In accordance with frozen BookNGo architectural guidelines, TypeScript is not introduced, and Redux is prohibited.*

---

## 4. Application Structure
The application follows a modular, feature-oriented structure with clean boundaries between cross-cutting concerns (common UI, api client, contexts) and domain feature modules (movies, shows, seat-selection, checkout, bookings).

```text
bookngo-frontend/
├── index.html
├── vite.config.js                 # Configured with @vitejs/plugin-react and @tailwindcss/vite
├── package.json                   # React, React Router, TanStack Query, Tailwind v4
└── src/
    ├── main.jsx                   # Application bootstrap and root DOM mount
    ├── App.jsx                    # Root component with Providers (QueryClient, Auth, Router)
    ├── api/                       # HTTP client and low-level API communication
    │   ├── client.js              # Central Axios/Fetch instance with interceptors
    │   ├── endpoints.js           # API route constants matching openapi.yaml
    │   └── errorHandler.js        # Standardized API error parser
    ├── assets/                    # Static images, cinematic banners, SVGs
    ├── components/                # Shared cross-feature UI components
    │   ├── ui/                    # Primitive design-system atoms (Buttons, Inputs, Badges, Modals)
    │   ├── feedback/              # Alert banners, ErrorBoundary, Skeleton loaders, Toast stack
    │   └── layout/                # MainLayout, Header, Footer, Breadcrumbs, Navigation
    ├── context/                   # Global React contexts (AuthContext, ToastContext)
    ├── features/                  # Domain-specific modules
    │   ├── auth/                  # Phone OTP login, Operator password login, profile hooks
    │   ├── movies/                # Movie grid, filters, movie detail banner, trailers
    │   ├── shows/                 # Showtime schedule selector, date picker, theatre list
    │   ├── seats/                 # Interactive SVG seat-map, legend, selection drawer
    │   ├── checkout/              # Hold countdown timer, payment method picker, processing
    │   └── bookings/              # Booking confirmation, digital ticket, booking history
    ├── hooks/                     # Common custom utility hooks (useCountdown, useDebounce)
    ├── routes/                    # Route configuration and route guards
    │   ├── AppRoutes.jsx          # Route tree definition
    │   ├── ProtectedRoute.jsx     # Authentication and role verification wrapper
    │   └── RoleRoute.jsx          # Fine-grained role access control
    ├── utils/                     # Formatting utilities, currency helpers, date parsers
    └── styles/
        └── index.css              # Tailwind base layers, CSS custom properties, keyframes
```

---

## 5. Folder and Module Organization Rules
1. **Feature Encapsulation**: Each domain directory under `src/features/<feature>/` encapsulates its own:
   - `components/`: Feature-specific visual components (e.g., `SeatMap.jsx`, `SeatItem.jsx`).
   - `api/`: TanStack Query hooks and mutations (e.g., `useSeatInventory.js`, `useCreateHold.js`).
   - `hooks/`: Feature-specific local UI controllers (e.g., `useSeatSelectionState.js`).
2. **Shared UI Purity**: Components in `src/components/ui/` must remain pure presentational components without direct TanStack Query dependencies or domain API calls.
3. **No Circular Imports**: Feature modules may import from `components/`, `context/`, `api/`, and `utils/`, but features must never import internal components from other peer features. Cross-feature integration happens at the page or route layout level.

---

## 6. Routing Architecture
BookNGo uses modern React Router declarative routing (using the currently installed `react-router` package). The route configuration separates public discovery flows from authenticated transactional flows.

### Route Map:
| Route Path | Access Level | Description |
|---|---|---|
| `/` | Public | Home landing page: featured movies, active releases, location picker |
| `/movies` | Public | Movie catalog with filter controls (language, genre) |
| `/movies/:movieId` | Public | Movie synopsis, duration, metadata, and "View Shows" CTA |
| `/movies/:movieId/shows` | Public | Date-based showtimes grouped by theatre |
| `/shows/:showId/seats` | Public / Auth Guarded on Proceed | Live seat-map; anonymous users can view and select seats |
| `/login` | Public (Guest only) | Authentication portal (OTP for customers, Password for operators) |
| `/checkout/:bookingId` | Authenticated (CUSTOMER) | Active hold review, countdown timer, payment selection |
| `/checkout/:bookingId/processing` | Authenticated (CUSTOMER) | Payment execution polling screen |
| `/bookings/:bookingId/confirmation` | Authenticated (CUSTOMER) | Post-payment success summary |
| `/bookings/:bookingId/ticket` | Authenticated (CUSTOMER) | Authoritative digital ticket with ticket code and show QR |
| `/history` | Authenticated (CUSTOMER) | Chronological list of user's past and upcoming bookings |
| `/bookings/:bookingId` | Authenticated (CUSTOMER/ADMIN) | Detailed view of an individual booking with cancellation CTA |
| `/profile` | Authenticated (All Roles) | User profile details and update form |
| `/unauthorized` | Public | 403 Forbidden explanation |
| `*` | Public | 404 Not Found error screen |

---

## 7. Public vs Authenticated Routes
- **Anonymous Discovery**: Browsing movies, filtering by genre/language, selecting showtimes, and viewing physical seat layouts does NOT require login. Forcing authentication prematurely causes high conversion drop-off.
- **Transactional Boundary**: Authentication is enforced at the precise moment the customer clicks **"Proceed to Book"** on the seat selection screen (`/shows/:showId/seats`). If unauthenticated, the client caches the pending selection (`showId` and selected `physicalSeatIds`) in session memory, redirects to `/login`, and returns the user to seat reservation immediately upon successful OTP verification.

---

## 8. Role-Aware Route Handling
The backend User Service enforces three roles: `CUSTOMER`, `THEATRE_OPERATOR`, and `ADMIN`.
- The `ProtectedRoute` component inspects the active session:
  1. Checks if a valid, unexpired JWT token exists.
  2. If missing or expired, redirects to `/login` with a `returnUrl` query parameter.
- The `RoleRoute` component inspects `user.roles`:
  1. Compares user roles against allowed roles defined on the route.
  2. If the user does not possess the requisite role, navigates to `/unauthorized` (HTTP 403 presentation).

---

## 9. API Client Architecture
All network interactions use a centralized API client module (`src/api/client.js`) wrapping standard `fetch` or `axios`.

### Client Core Responsibilities:
1. **Base URL Resolution**: Defaults to `/api/v1` via Vite's proxy configuration in development and the production gateway domain in deployment.
2. **Authentication Interceptor**: Automatically attaches `Authorization: Bearer <token>` to request headers if a valid token exists in the Auth state.
3. **Idempotency Key Injection**: Automatically generates and attaches a unique UUIDv4 `Idempotency-Key` header for critical state-altering mutations:
   - `POST /api/v1/shows/{showId}/holds`
   - `POST /api/v1/payments`
4. **Standard Error Normalization**: Inspects HTTP error responses and transforms them into a uniform `ApiError` structure:
   ```javascript
   export class ApiError extends Error {
     constructor(status, code, message, details = []) {
       super(message);
       this.status = status;
       this.code = code;
       this.details = details;
     }
   }
   ```
5. **Gateway Fault Interception**: Translates gateway-level HTTP 502 (Bad Gateway), 503 (Service Unavailable), and 504 (Gateway Timeout) into user-friendly status responses indicating transient microservice unavailability.

---

## 10. API Gateway Integration
The frontend strictly communicates with the backend via the unified API Gateway at port `8080`.
- All requests target `/api/v1/**`.
- The frontend never references individual microservice hostnames (`user-service`, `booking-service`, etc.) or ports (`8081`-`8086`).
- Internal endpoints under `/internal/v1/**` are inaccessible to the browser and are blocked at the gateway level.

---

## 11. Authentication and Session Architecture
BookNGo implements JWT-based authentication without session cookies:
1. **Authentication Flows**:
   - **Customer Flow**: Phone number submission via `POST /api/v1/auth/otp/request` followed by OTP verification via `POST /api/v1/auth/otp/verify`. Returns `AuthTokenResponse` containing JWT token, expiration, and user object.
   - **Operator/Admin Flow**: Username/password submission via `POST /api/v1/auth/login`. Returns `AuthTokenResponse`.
2. **Token Storage**:
   - The JWT is stored in application memory (`AuthContext`) and mirrored to `localStorage` (or `sessionStorage`) for session persistence across page refreshes.
   - A dedicated `useAuth()` hook exposes `{ user, token, isAuthenticated, login, logout, isCustomer, isOperator, isAdmin }`.
3. **Session Expiration Handling**:
   - The API client intercepts any HTTP 401 Unauthorized response from a protected route.
   - Upon receiving a 401, the client automatically clears stored credentials, dispatches an `AUTH_EXPIRED` event to the `AuthContext`, displays a toast notification ("Your session has expired. Please log in again."), and redirects to `/login`.

---

## 12. TanStack Query Architecture
TanStack Query v5 is the authoritative server-state synchronizer.

### Query Client Defaults:
```javascript
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 2,       // 2 minutes for general catalog data
      gcTime: 1000 * 60 * 10,         // 10 minutes cache garbage collection
      retry: (failureCount, error) => {
        // Never retry 401, 403, 404, or 409 Conflict
        if ([401, 403, 404, 409, 422].includes(error?.status)) return false;
        return failureCount < 2;
      },
      refetchOnWindowFocus: false,
    },
  },
});
```

### Strict Optimistic Update Prohibition:
TanStack Query provides `onMutate` optimistic update hooks. **In BookNGo, optimistic updates are strictly forbidden for holds, bookings, and payments.**
- Creating a hold must await server confirmation (`201 Created`).
- Cancelling a booking must await server confirmation (`200 OK`).
- Initiating payment must await server confirmation (`201 Created`).

---

## 13. Local UI State Architecture
Local UI state manages transient, non-persisted user interactions using standard React `useState` and `useReducer`:
1. **Seat Selection Draft**: An array of selected `physicalSeatId` strings held in local state while the user interacts with the seat map.
2. **Filters & Search Inputs**: Current text in search bars, active category filters, selected date chips.
3. **Modal & Drawer Visibility**: Boolean flags controlling confirmation dialogs, error alerts, and trailer modals.
4. **Idempotency Lifecycles**: Client-generated UUIDs tied to the active user interaction session to avoid duplicate POST submissions.

---

## 14. Component Architecture
Components are structured according to functional responsibility:
- **Atoms**: Pure, low-level primitives (`Button`, `Input`, `Badge`, `Spinner`, `Card`).
- **Molecules**: Combinations of atoms (`SearchBar`, `MovieCard`, `ShowtimePill`, `CountdownBadge`).
- **Organisms**: Domain-rich composites (`SeatMap`, `CheckoutSummary`, `TicketPass`, `BookingList`).
- **Templates / Layouts**: Page shell structures (`MainLayout`, `AuthLayout`).

---

## 15. Page Architecture
Page components reside in `src/features/<feature>/pages/` and act as the top-level route targets:
- Responsibilities:
  1. Extract route parameters (`movieId`, `showId`, `bookingId`) via `useParams()`.
  2. Invoke feature-level query hooks to fetch server data.
  3. Render loading skeletons while data is resolving.
  4. Render structured error banners if queries fail.
  5. Orchestrate child organism components.

---

## 16. Shared Component Strategy
All shared components live under `src/components/ui/` and share strict design token alignment:
- Standardized props: `variant` (`primary`, `secondary`, `outline`, `ghost`, `danger`), `size` (`sm`, `md`, `lg`), `disabled`, `isLoading`.
- Zero hardcoded colors: all styles leverage Tailwind utility classes tied to semantic theme tokens defined in `design-system.md`.

---

## 17. Error Handling Architecture
BookNGo implements layered error containment:
1. **React Error Boundary**: Top-level `GlobalErrorBoundary` catches unhandled rendering errors and displays a recovery screen ("Something went wrong") with a "Reload Page" CTA.
2. **Query-Level Error Displays**: Components rendering queries display contextual inline error cards (`InlineAlert`) containing retry buttons (`query.refetch()`).
3. **Toast Notifications**: Ephemeral operational errors (e.g., "Seat hold conflict: One or more selected seats are no longer available") are broadcast via a fixed floating Toast stack.
4. **Conflict Resolution**: Concurrency conflicts (HTTP 409) automatically invalidate the active seat inventory query and trigger an immediate visual refresh of the seat map.

---

## 18. Loading and Skeleton Architecture
- **No Full-Page Blocking Spinners**: To avoid jarring layout shifts, pages use content-matched skeleton components (`MovieCardSkeleton`, `ShowtimeSkeleton`, `SeatMapSkeleton`).
- **Background Refetches**: When TanStack Query refetches in the background (e.g., polling seat inventory), visual content remains fully interactive without showing loaders; only subtle indicator pills ("Updating live seats...") indicate background sync.

---

## 19. Empty-State Architecture
Every list or collection view defines an explicit, polished empty state:
- Components: Illustrative Lucide icon, clear explanatory title, contextual subtext, and a primary call-to-action button.
- Examples:
  - No movies found matching genre -> "No Movies Found" with "Clear Filters" button.
  - No showtimes scheduled for date -> "No Shows on Selected Date" with "Check Tomorrow" CTA.
  - No bookings in history -> "No Bookings Yet" with "Explore Movies" CTA.

---

## 20. Form Architecture
Forms (OTP verification, profile edit, operator password login) use controlled React components:
- Pure React state with custom input validators.
- Real-time client-side formatting (e.g., formatting phone numbers to international standard `+91...`).
- Explicit disabled states during mutation execution (`isSubmitting` / `isPending`).
- Explicit server validation error binding: field-specific validation errors from HTTP 400 responses are rendered directly below the corresponding input.

---

## 21. Responsive Architecture
Mobile-first layout system designed across standard breakpoints:
- `sm`: 640px (large phones)
- `md`: 768px (tablets)
- `lg`: 1024px (laptops)
- `xl`: 1280px (desktop monitors)
- Special responsive treatments:
  - Movie catalog: 1 column (mobile), 2 columns (tablet), 4 columns (desktop).
  - Seat-map: Pinch-to-zoom and pan container on mobile touchscreens; full expanded layout on desktop.
  - Checkout/Seat Selection footer: Fixed bottom sheet drawer on mobile; sticky right-hand column on desktop.

---

## 22. Accessibility Principles (a11y)
1. **Semantic HTML**: Mandatory usage of `<header>`, `<main>`, `<nav>`, `<section>`, `<article>`, `<button>`.
2. **Keyboard Navigability**: Full tab index traversal across all interactive elements. Modals implement focus traps.
3. **Seat Map Accessibility**: Every seat button exposes `aria-label="Row A Seat 10, Regular Category, Price ₹250, Available"`, `role="checkbox"`, and `aria-checked="true|false"`.
4. **Live Regions**: Hold countdown timer and live seat conflict announcements use `aria-live="polite"` to alert screen reader users without interrupting active focus.
5. **Color Contrast**: All typography and status indicators meet WCAG 2.1 AA contrast ratio requirements (minimum 4.5:1 against dark cinematic backgrounds).

---

## 23. Frontend Security Boundaries
1. **Untrusted Client Assumption**: The frontend never assumes client-side checks provide security. All permissions and ownership checks are validated by the backend microservices.
2. **Zero Hardcoded Secrets**: Absolutely no database credentials, JWT signing keys, payment provider private keys, or microservice internal URLs exist in frontend code.
3. **Cross-Site Scripting (XSS) Mitigation**: React native JSX escaping is strictly maintained. Usage of `dangerouslySetInnerHTML` is prohibited.
4. **JWT Hygiene**: Tokens are stored solely for session identity and transmitted exclusively via the `Authorization: Bearer` HTTP header. Tokens are never appended to URLs or query parameters.

---

## 24. Backend/Frontend Contract Boundary
The frontend strictly adheres to `docs/03-api/openapi.yaml`.
- All URL paths, HTTP verbs, payload parameters, headers, and status codes must match the contract exactly.
- Missing attributes or composite requirements are explicitly documented as **Contract Gaps / Dependencies** (see Section 28).

---

## 25. Testing Architecture
1. **Unit & Component Testing**: Vitest and React Testing Library for testing pure utility functions, custom hooks, and isolated component rendering.
2. **API Mocking**: Mock Service Worker (MSW) intercepts network calls during testing to simulate OpenAPI contracts, including edge cases (HTTP 409 seat conflicts, HTTP 504 timeouts, HTTP 401 token expirations).
3. **End-to-End Testing**: Playwright tests covering critical paths:
   - Movie discovery -> Showtime selection -> Seat Map selection -> OTP Login -> Hold creation -> Mock Payment -> Ticket generation.

---

## 26. Performance Considerations
1. **Code Splitting & Lazy Loading**: Route components are dynamically imported using `React.lazy()` and wrapped in `Suspense` to keep initial bundle size below 150KB gzip.
2. **Asset Optimization**: Movie posters and banners use responsive `srcset` and `loading="lazy"`.
3. **Debounced Interactions**: Search query inputs and seat-map pan/zoom calculations are debounced to prevent unnecessary layout recalculations and network spam.

---

## 27. Frontend Implementation Rules
### Strict Rules (DOs and DON'Ts):
- **DO NOT** create a hold when a user simply clicks a seat on the seat map.
- **DO NOT** claim a booking is confirmed until `Booking.status === 'CONFIRMED'`.
- **DO NOT** connect directly to ports `8081`, `8082`, `8083`, `8084`, `8085`, or `8086`.
- **DO NOT** introduce TypeScript, Redux, or heavy UI frameworks (e.g., Material UI, Ant Design).
- **DO NOT** create client-side timers that claim authoritative hold expiration.
- **DO NOT** send `amount` or `currency` from the frontend to `POST /api/v1/payments`; payment initiation only accepts `{ bookingId, providerName }`.
- **DO NOT** hard-code generic payment provider names; use the verified simulation provider names (`SIM_SUCCESS`, `SIM_FAILURE`, `SIM_CANCELLED`, `SIM_UNKNOWN`).
- **DO NOT** attempt city filtering on Movie Service endpoints; city filtering applies strictly to theatre and show discovery (`GET /api/v1/theatres?city=...`, `GET /api/v1/shows?city=...`).
- **DO NOT** claim or assume external SMS or email notifications; BookNGo has no notification microservice, and booking confirmation is surfaced exclusively via the UI and ticket views.
- **DO NOT** claim cryptographic signature guarantees for digital ticket QR codes; the QR code is a visual encoding of `ticketCode` for gate admission.
- **DO** reuse the same `Idempotency-Key` when retrying a failed hold creation or payment initiation.
- **DO** invalidate the seat inventory query immediately upon receiving an HTTP 409 Conflict.
- **DO** clear authentication state and redirect to `/login` upon receiving HTTP 401.

---

## 28. Contract Gaps / Dependencies
1. **Composite Booking Details (`GET /api/v1/bookings/me`)**:
   - *Current Schema*: `Booking` contains `showId`, `userId`, `totalAmount`, `status`, and `bookingSeats`. It lacks movie metadata (`title`, `posterReference`), theatre metadata (`theatreName`, `city`), and show metadata (`startsAt`, `screenName`).
   - *Frontend Coordination*: The frontend coordinates queries via TanStack Query (`booking` -> `show` -> `movie` and `theatre`) to resolve display details without altering backend contracts.
2. **Ticket Historical Snapshot Structure**:
   - *Current Schema*: `Ticket.historicalSnapshot` is typed as an arbitrary `object` in `openapi.yaml`.
   - *Contract Dependency*: Frontend expects `historicalSnapshot` to provide `{ movieTitle, theatreName, screenName, showStartsAt, seatLabels, totalAmount, currency, ticketCode }` matching the backend persistence schema.
3. **Supported Cities Discovery**:
   - *Current Schema*: `GET /api/v1/theatres` accepts a `city` query parameter, but there is no dedicated endpoint returning the list of active cities.
   - *Frontend Implementation Workaround*: Frontend extracts distinct cities from the public theatres listing or maintains a client-side supported cities list.

---

## 29. Implementation Phasing and Scope
While this documentation provides the complete, authoritative specification for the full platform, frontend implementation is executed incrementally:
- **Phase 1 (Foundation & Discovery)**:
  - Establish Vite + React + Tailwind v4 design system foundation and root layout.
  - Implement API client with error handling and gateway routing.
  - Implement Movie Catalog and Movie Details discovery (`/` and `/movies`).
- **Phase 2 (Showtimes & Authentication)**:
  - Implement Date-based Theatre & Show Selection (`/movies/:movieId/shows`).
  - Implement Phone OTP Customer Authentication (`/login`).
- **Phase 3 (Seat Map & Concurrency)**:
  - Implement Interactive Seat Map with 5-second polling and local draft selection.
  - Implement Atomic Hold Creation and 409 Conflict handling.
- **Phase 4 (Checkout, Payment Simulation & Tickets)**:
  - Implement 5-minute Hold countdown and 2-minute Payment Grace polling screen.
  - Integrate verified payment simulations (`SIM_SUCCESS`, `SIM_FAILURE`, `SIM_CANCELLED`, `SIM_UNKNOWN`).
  - Implement Booking Confirmation and Digital Ticket Pass (`/bookings/:bookingId/ticket`).
- **Phase 5 (History & Cancellation)**:
  - Implement Customer Booking History and cancellation flows.
#END
