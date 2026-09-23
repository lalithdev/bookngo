#START
# BookNGo User Flows Specification

## 1. Overview
This document specifies the end-to-end user journeys for the BookNGo platform. Each flow defines the actor, starting conditions, user interactions, frontend presentation logic, precise API interactions matching `docs/03-api/openapi.yaml`, success/failure outcomes, navigation transitions, and authoritative backend states.

---

## 2. Core User Flows (A through Y)

### Flow A: Anonymous Movie Discovery
- **Actor**: Guest / Anonymous Customer
- **Starting Condition**: Browser loads `http://localhost:8080/` (or client dev root `/`).
- **User Actions**: User lands on the homepage, selects a city from the location dropdown, or scrolls through movie carousels.
- **Frontend Behavior**:
  - Displays location selector, search input, and movie grid.
  - Queries active movies filtered by `status=ACTIVE` and chosen city.
- **API Interaction**:
  - `GET /api/v1/movies?status=ACTIVE`
  - `GET /api/v1/theatres?city={selectedCity}`
- **Authoritative Backend State**: Read-only query against `movie_db.movies`.
- **Successful Outcome**: Movie cards display with posters, titles, languages, and genres.
- **Failure Outcomes**: HTTP 500 displays an error banner with a "Retry" button.
- **Navigation Outcome**: Stays on `/` or `/movies`.

---

### Flow B: Movie Details
- **Actor**: Customer (Guest or Authenticated)
- **Starting Condition**: Viewing movie list.
- **User Actions**: Clicks on a specific movie card.
- **Frontend Behavior**: Navigates to `/movies/:movieId`, renders hero banner, duration, genres, release date, and synopsis.
- **API Interaction**:
  - `GET /api/v1/movies/{movieId}`
- **Authoritative Backend State**: Read-only query against `movie_db.movies`.
- **Successful Outcome**: Movie details render with a primary CTA button: **"Book Tickets"**.
- **Failure Outcomes**: HTTP 404 displays `Movie Not Found` error card with "Browse All Movies" CTA.
- **Navigation Outcome**: User clicks "Book Tickets", navigating to `/movies/:movieId/shows`.

---

### Flow C: Theatre and Showtime Discovery
- **Actor**: Customer
- **Starting Condition**: Navigated to `/movies/:movieId/shows`.
- **User Actions**: Selects a target date from the horizontal date picker (e.g., Today, Tomorrow, +2 Days) and optionally filters by format (2D, 3D, IMAX) or language.
- **Frontend Behavior**: Displays list of theatres offering screenings of the movie on that date, along with showtime chips, cancellation badges, and pricing hints.
- **API Interaction**:
  - `GET /api/v1/shows?movieId={movieId}&city={city}&date={date}`
  - `GET /api/v1/theatres/{theatreId}` (to resolve theatre name and location)
  - `GET /api/v1/shows/{showId}/cancellation-policy`
- **Authoritative Backend State**: Read-only query across `show_db.shows` and `theatre_db.theatres`.
- **Successful Outcome**: Showtimes render as clickable time pills (e.g., `07:30 PM`).
- **Failure Outcomes**: Empty results render `No Shows Scheduled for this Date` empty state.
- **Navigation Outcome**: User clicks a showtime pill, navigating to `/shows/:showId/seats`.

---

### Flow D: Customer Authentication (Phone OTP)
- **Actor**: Unauthenticated Customer
- **Starting Condition**: Customer attempts to book seats, access booking history, or manually clicks "Sign In".
- **User Actions**: Enters 10-digit mobile number, clicks "Send OTP", receives simulated/delivered 6-digit code, enters code, and clicks "Verify & Continue".
- **Frontend Behavior**:
  - Step 1: Validates phone format (`^\+[1-9]\d{1,14}$`), disables button, shows spinner.
  - Step 2: Renders 6-digit OTP input boxes with 60-second resend countdown.
  - Step 3: On submission, stores returned JWT in `AuthContext` and `localStorage`.
- **API Interaction**:
  - Step 1: `POST /api/v1/auth/otp/request` with `{ "phoneNumber": "+919876543210" }`
  - Step 2: `POST /api/v1/auth/otp/verify` with `{ "phoneNumber": "+919876543210", "otpCode": "123456" }`
- **Authoritative Backend State**: `user_db.otp_verifications` status changes from `PENDING` to `VERIFIED`; User record is created/retrieved in `user_db.users` with role `CUSTOMER`.
- **Successful Outcome**: Server returns `AuthTokenResponse` containing JWT.
- **Failure Outcomes**:
  - Invalid OTP (HTTP 400): Displays "Invalid verification code. Please check and try again."
  - Rate limited (HTTP 429): Displays "Too many OTP requests. Please wait 5 minutes."
- **Navigation Outcome**: Redirects user to their stored `returnUrl` (e.g., pending seat selection or checkout).

---

### Flow E: Seat Selection (Local Draft)
- **Actor**: Customer
- **Starting Condition**: Landed on `/shows/:showId/seats`.
- **User Actions**: Inspects screen arc and seat grid. Clicks up to 6 available seats.
- **Frontend Behavior**:
  - Loads physical seat layout and current inventory.
  - Clicking an `AVAILABLE` seat toggles its state locally to `SELECTED` (green).
  - Toggling an already `SELECTED` seat returns it to `AVAILABLE`.
  - Enforces local cap: attempting to select a 7th seat displays a toast: "You can select up to 6 seats per booking."
  - Computes running price estimate in sticky drawer based on category pricing.
  - **CRITICAL**: No backend hold is created during this local interaction.
- **API Interaction**:
  - `GET /api/v1/shows/{showId}` (retrieves `screenId`)
  - `GET /api/v1/screens/{screenId}/seats` (retrieves physical seat layout)
  - `GET /api/v1/shows/{showId}/seat-inventory` (retrieves real-time availability: `AVAILABLE`, `HELD`, `BOOKED`)
  - `GET /api/v1/shows/{showId}/pricing` (retrieves pricing per seat category)
- **Authoritative Backend State**: Unchanged (`show_db` and `booking_db` remain unaltered).
- **Successful Outcome**: User sees selected seats (`A10, A11`), seat count, and total estimated price in sticky bottom bar.
- **Failure Outcomes**: Physical layout or inventory failed to load -> renders error alert with "Reload Seat Map".
- **Navigation Outcome**: User clicks "Proceed to Book" (moves to Flow F).

---

### Flow F: Proceed to Book
- **Actor**: Customer with 1 to 6 seats selected.
- **Starting Condition**: Sticky summary displays selected seats and total price.
- **User Actions**: Clicks **"Proceed to Book"**.
- **Frontend Behavior**:
  - Checks if user is authenticated:
    - If unauthenticated: Stores selected seat IDs and `showId` in session state and redirects to `/login?returnUrl=/shows/:showId/seats`.
    - If authenticated: Disables button, sets loading spinner, and immediately invokes Flow G (Hold Creation).
- **API Interaction**: None (or session redirect).
- **Authoritative Backend State**: Unchanged.
- **Successful Outcome**: Transitions to hold creation mutation.
- **Failure Outcomes**: None.
- **Navigation Outcome**: Initiates Flow G.

---

### Flow G: Seat Hold Creation
- **Actor**: Authenticated Customer
- **Starting Condition**: User confirmed seat selection.
- **User Actions**: Triggered automatically via "Proceed to Book".
- **Frontend Behavior**:
  - Generates a UUIDv4 idempotency key.
  - Sends hold creation request with `Idempotency-Key` header and payload `{ physicalSeatIds: [...] }`.
  - Disables UI to prevent multiple submissions.
- **API Interaction**:
  - `POST /api/v1/shows/{showId}/holds`
  - Header: `Idempotency-Key: <uuid-v4>`
  - Header: `Authorization: Bearer <JWT>`
  - Body: `{ "physicalSeatIds": ["uuid-seat-1", "uuid-seat-2"] }`
- **Authoritative Backend State**:
  - Atomic transaction in `booking_db`:
    1. Locks and updates `show_seat_inventory` rows from `AVAILABLE` to `HELD`.
    2. Inserts `bookings` record in `INITIATED` status.
    3. Inserts `seat_holds` record in `ACTIVE` status with 5-minute `normal_expires_at`.
- **Successful Outcome**: Returns HTTP 201 Created with `SeatHoldResponse` (`holdId`, `bookingId`, `normalExpiresAt`, `totalAmount`, `currency`).
- **Failure Outcomes**:
  - Seat Conflict (HTTP 409): Another user took one or more seats. Invokes Flow O.
  - Invalid Count (HTTP 400 / 422): Exceeded 6 seats or show inactive.
- **Navigation Outcome**: On HTTP 201, navigates to `/checkout/:bookingId`.

---

### Flow H: Payment Initiation and Checkout
- **Actor**: Authenticated Customer with an active hold.
- **Starting Condition**: Viewing `/checkout/:bookingId`.
- **User Actions**:
  - Inspects booking summary (movie, theatre, seats, authoritative price from hold response, cancellation terms).
  - Observes the active 5-minute countdown timer.
  - Selects payment simulation provider (e.g., standard simulation: `SIM_SUCCESS`, or test options: `SIM_FAILURE`, `SIM_CANCELLED`, `SIM_UNKNOWN`).
  - Clicks **"Pay ₹Total"**.
- **Frontend Behavior**:
  - Generates a unique UUIDv4 idempotency key for payment.
  - Disables payment button and starts spinner.
  - **CRITICAL**: The frontend sends ONLY `bookingId` and `providerName`. The frontend never sends `amount` or `currency`; the backend Payment Service fetches the authoritative payable amount directly from Booking Service.
- **API Interaction**:
  - `POST /api/v1/payments`
  - Header: `Idempotency-Key: <uuid-v4>`
  - Header: `Authorization: Bearer <JWT>`
  - Body: `{ "bookingId": "<booking-id>", "providerName": "SIM_SUCCESS" }`
- **Authoritative Backend State**:
  - `payment_db.payments` created in `INITIATED` status.
  - Payment Service calls Booking Service internal endpoint to set 2-minute payment grace period on hold.
  - `booking_db.bookings` status updates to `PAYMENT_PENDING`.
- **Successful Outcome**: Returns HTTP 201 Created with `PaymentResponse` (`paymentId`, `status: INITIATED`, `paymentGraceExpiresAt`).
- **Failure Outcomes**:
  - Hold Expired (HTTP 409): Hold expired before payment initiation. Enters Flow N.
- **Navigation Outcome**: Navigates to `/checkout/:bookingId/processing?paymentId={paymentId}`.

---

### Flow I: Successful Booking and Payment Processing
- **Actor**: Authenticated Customer
- **Starting Condition**: Landed on `/checkout/:bookingId/processing`.
- **User Actions**: Waits as payment completes.
- **Frontend Behavior**:
  - Displays payment processing animation.
  - Polls payment status endpoint every 2 seconds (up to 30 seconds).
  - Stops polling immediately when status is terminal (`SUCCESS`).
- **API Interaction**:
  - Polling: `GET /api/v1/payments/{paymentId}/status`
- **Authoritative Backend State**:
  - Provider callback or internal completion updates `payment_db.payments` to `SUCCESS`.
  - `booking_db.bookings` updates from `PAYMENT_PENDING` to `CONFIRMED`.
  - `booking_db.tickets` record is inserted with status `ISSUED`.
  - `booking_db.show_seat_inventory` updates from `HELD` to `BOOKED`.
- **Successful Outcome**: Response returns `{ "status": "SUCCESS" }`.
- **Failure Outcomes**: Payment failure or cancellation (Flows P, Q).
- **Navigation Outcome**: Navigates immediately to `/bookings/:bookingId/confirmation`.

---

### Flow J: Ticket Viewing
- **Actor**: Authenticated Customer
- **Starting Condition**: Completed booking or clicked ticket from History.
- **User Actions**: Navigates to `/bookings/:bookingId/ticket`.
- **Frontend Behavior**:
  - Fetches authoritative ticket from Booking Service.
  - Renders ticket pass layout: QR code representation (visually encoding the backend `ticketCode` for gate admission, without cryptographic claims), alphanumeric `ticketCode`, movie details, showtime, seat labels, and price.
  - Provides "Print / Save PDF" and "Back to Bookings" CTAs.
- **API Interaction**:
  - `GET /api/v1/bookings/{bookingId}/ticket`
  - Header: `Authorization: Bearer <JWT>`
- **Authoritative Backend State**: Read-only query against `booking_db.tickets`.
- **Successful Outcome**: Digital ticket rendered with `status: ISSUED`.
- **Failure Outcomes**: HTTP 404 (Ticket not issued yet) redirects back to booking details.
- **Navigation Outcome**: Stays on ticket screen or returns to `/history`.

---

### Flow K: Booking History
- **Actor**: Authenticated Customer
- **Starting Condition**: Clicks "My Bookings" in navigation header.
- **User Actions**: Views list of past and upcoming bookings.
- **Frontend Behavior**:
  - Displays chronological list of bookings sorted by date descending.
  - Each item shows booking date, status badge (`CONFIRMED`, `CANCELLED`, `EXPIRED`), total amount, and seats.
  - The client coordinates queries via TanStack Query (`booking` -> `show` -> `movie` and `theatre`) to resolve rich display metadata without requiring backend modifications.
- **API Interaction**:
  - `GET /api/v1/bookings/me`
  - Header: `Authorization: Bearer <JWT>`
  - Coordinated: `GET /api/v1/shows/{showId}`, `GET /api/v1/movies/{movieId}`, `GET /api/v1/theatres/{theatreId}` (as needed/cached)
- **Authoritative Backend State**: Read-only query against `booking_db.bookings`.
- **Successful Outcome**: Cards render with "View Ticket" (if `CONFIRMED`) or "View Details".
- **Failure Outcomes**: HTTP 401 triggers logout/redirect to `/login`.
- **Navigation Outcome**: Clicking an item navigates to `/bookings/:bookingId`.

---

### Flow L: Booking Details
- **Actor**: Authenticated Customer
- **Starting Condition**: Navigated to `/bookings/:bookingId`.
- **User Actions**: Reviews full booking record, payment status, show details, and cancellation options.
- **Frontend Behavior**:
  - Fetches booking record and show cancellation policy.
  - If booking is `CONFIRMED` and cancellation policy allows it before the deadline, displays **"Cancel Booking"** button.
  - If show has passed or policy is `NON_CANCELLABLE`, "Cancel Booking" is hidden or disabled with reason tooltip.
- **API Interaction**:
  - `GET /api/v1/bookings/{bookingId}`
  - `GET /api/v1/shows/{showId}/cancellation-policy`
- **Authoritative Backend State**: Read-only query against `booking_db.bookings`.
- **Successful Outcome**: Full breakdown rendered with status-appropriate CTAs.
- **Failure Outcomes**: HTTP 403 Forbidden if user is not the owner of the booking.
- **Navigation Outcome**: Stays on screen or opens cancellation modal.

---

### Flow M: Booking Cancellation and Refund
- **Actor**: Authenticated Customer
- **Starting Condition**: Viewing an eligible `CONFIRMED` booking.
- **User Actions**: Clicks "Cancel Booking", confirms in the modal ("Are you sure you want to cancel? Refund of ₹Amount will be initiated according to theatre policy.").
- **Frontend Behavior**:
  - Sends cancellation request.
  - On success, updates UI status to `CANCELLED` and displays refund notification.
  - Queries refund status.
- **API Interaction**:
  - `POST /api/v1/bookings/{bookingId}/cancel`
  - `GET /api/v1/payments/{paymentId}/refund`
- **Authoritative Backend State**:
  - `booking_db.bookings` updates to `CANCELLED`.
  - `booking_db.tickets` status updates to `CANCELLED`.
  - `booking_db.show_seat_inventory` seats revert from `BOOKED` to `AVAILABLE`.
  - `payment_db.refund_reversals` record created in `INITIATED` or `REFUND_PENDING`.
- **Successful Outcome**: Modal closes, booking badge changes to `CANCELLED`, refund tracking pill displays: `"Refund of ₹Total initiated"`.
- **Failure Outcomes**: HTTP 409/422 (Cancellation deadline passed) -> error toast: "Cancellation deadline has passed for this show."
- **Navigation Outcome**: Stays on `/bookings/:bookingId` with updated cancelled state.

---

### Flow N: Hold Expiry
- **Actor**: Customer on Seat Selection or Checkout screen.
- **Starting Condition**: 5-minute countdown reaches zero without payment initiation.
- **User Actions**: Customer attempts to click "Pay" after expiry, or countdown hits `00:00`.
- **Frontend Behavior**:
  - Timer displays "Hold Expired".
  - Disables checkout action buttons.
  - Displays modal: `"Your seat hold has expired. The seats have been released back to other moviegoers."` with CTA: `"Select Seats Again"`.
- **API Interaction**:
  - `GET /api/v1/holds/{holdId}` returns `{ "status": "EXPIRED" }`.
- **Authoritative Backend State**: `seat_holds` status is `EXPIRED`; `show_seat_inventory` is reclaimed as `AVAILABLE`.
- **Successful Outcome**: Customer understands seats were released.
- **Failure Outcomes**: Attempting payment returns HTTP 409 Conflict.
- **Navigation Outcome**: Navigates back to `/shows/:showId/seats` and refreshes inventory.

---

### Flow O: Seat Conflict During Hold Acquisition
- **Actor**: Customer attempting to hold seats.
- **Starting Condition**: Multiple users select the same seat (e.g., A10) simultaneously.
- **User Actions**: Customer clicks "Proceed to Book".
- **Frontend Behavior**:
  - Server returns HTTP 409 Conflict.
  - UI intercepts the 409 response.
  - Displays high-priority toast: `"Seat Conflict: Seat A10 was just acquired by another customer. Please choose alternative seats."`
  - Automatically invalidates TanStack Query cache for `seat-inventory`.
  - Refetches live inventory: A10 now renders as `HELD` / unavailable.
  - Deselects the conflicting seat from local selection.
- **API Interaction**:
  - `POST /api/v1/shows/{showId}/holds` -> returns `409 Conflict`
  - `GET /api/v1/shows/{showId}/seat-inventory` (immediate refetch)
- **Authoritative Backend State**: Another user holds the seat; current user has no hold.
- **Successful Outcome**: User is prevented from making an invalid booking and prompted to re-select.
- **Failure Outcomes**: None.
- **Navigation Outcome**: Remains on `/shows/:showId/seats`.

---

### Flow P: Payment Failure
- **Actor**: Customer during payment execution.
- **Starting Condition**: Payment provider declines transaction (insufficient funds, bank decline).
- **User Actions**: Customer awaits payment processing.
- **Frontend Behavior**:
  - Polling returns `{ "status": "FAILURE" }`.
  - Stops polling.
  - Displays payment failure screen: `"Payment Failed: Your bank declined the transaction."`
  - If hold is still within valid grace period, provides **"Retry Payment"** CTA.
  - If hold has expired, provides **"Back to Seat Selection"** CTA.
- **API Interaction**:
  - `GET /api/v1/payments/{paymentId}/status` -> `{ "status": "FAILURE" }`
- **Authoritative Backend State**:
  - `payment_db.payments` is `FAILURE`.
  - `booking_db.bookings` updates to `PAYMENT_FAILED`.
  - Associated seat inventory is released back to `AVAILABLE`.
- **Successful Outcome**: User is informed without duplicate charge.
- **Failure Outcomes**: None.
- **Navigation Outcome**: Remains on failure screen with recovery options.

---

### Flow Q: Payment Cancellation by Customer
- **Actor**: Customer at checkout.
- **Starting Condition**: Viewing active checkout screen.
- **User Actions**: Clicks "Cancel & Release Seats".
- **Frontend Behavior**:
  - Displays confirmation dialog: `"Are you sure? Your held seats will be released immediately."`
  - On confirm, calls release hold endpoint.
- **API Interaction**:
  - `DELETE /api/v1/holds/{holdId}`
  - Header: `Authorization: Bearer <JWT>`
- **Authoritative Backend State**: `seat_holds` updates to `RELEASED`; `show_seat_inventory` reverts immediately to `AVAILABLE`.
- **Successful Outcome**: Returns HTTP 204 No Content.
- **Failure Outcomes**: If already expired, returns 404/409 (safe to ignore).
- **Navigation Outcome**: Redirects back to `/movies/:movieId/shows`.

---

### Flow R: Payment Timeout
- **Actor**: Customer during checkout.
- **Starting Condition**: Payment grace window (2 minutes) elapses without terminal response from payment provider.
- **User Actions**: Awaits processing.
- **Frontend Behavior**:
  - Polling exceeds maximum timeout threshold.
  - Displays warning screen: `"Payment Processing Timed Out. We are checking the final status with your payment provider."`
  - Provides a manual **"Check Status"** button.
- **API Interaction**:
  - `GET /api/v1/payments/{paymentId}/status` -> returns `{ "status": "TIMEOUT" }`
- **Authoritative Backend State**: `payments` status is `TIMEOUT`. Background reconciliation job is queued.
- **Successful Outcome**: System prevents blind duplicate payments.
- **Failure Outcomes**: None.
- **Navigation Outcome**: Stays on status resolution screen.

---

### Flow S: Payment UNKNOWN State
- **Actor**: Customer during checkout.
- **Starting Condition**: Network drop or provider gateway anomaly leaves payment outcome ambiguous.
- **User Actions**: Clicks "Check Status" or refreshes page.
- **Frontend Behavior**:
  - Displays informative amber alert:
    `"Transaction Status Pending: We could not verify whether your bank processed this charge. To protect you from duplicate charges, we are reconciling the status. Please do not re-attempt payment immediately."`
  - Provides link to "My Bookings" to check later.
- **API Interaction**:
  - `GET /api/v1/payments/{paymentId}/status` -> `{ "status": "UNKNOWN" }`
- **Authoritative Backend State**: `payments` status is `UNKNOWN`. Idempotency protects against duplicate charges.
- **Successful Outcome**: User is safeguarded against duplicate deductions.
- **Failure Outcomes**: None.
- **Navigation Outcome**: Provides CTA to `/history`.

---

### Flow T: Payment Succeeds but Confirmation/Retrieval Interrupted
- **Actor**: Customer
- **Starting Condition**: Bank processes payment successfully, but customer browser loses internet connection before confirmation redirect.
- **User Actions**: Re-opens BookNGo application after internet reconnects.
- **Frontend Behavior**:
  - Customer navigates to "My Bookings" (`/history`).
  - History query fetches latest bookings from backend.
  - The booking displays as `CONFIRMED`.
  - Customer clicks "View Ticket" and accesses digital ticket pass.
- **API Interaction**:
  - `GET /api/v1/bookings/me` -> returns booking with `status: CONFIRMED`
  - `GET /api/v1/bookings/{bookingId}/ticket` -> returns ticket code and QR snapshot
- **Authoritative Backend State**: Backend webhook processed payment independently of client presence; booking confirmed and ticket issued in `booking_db`.
- **Successful Outcome**: Zero lost tickets; complete customer recovery without support intervention.
- **Failure Outcomes**: None.
- **Navigation Outcome**: User successfully views ticket on `/bookings/:bookingId/ticket`.

---

### Flow U: Session Expiry During Active Flow
- **Actor**: Authenticated Customer
- **Starting Condition**: JWT token expires while browsing or completing checkout.
- **User Actions**: Clicks an action requiring authentication.
- **Frontend Behavior**:
  - API client intercepts HTTP 401 Unauthorized.
  - Clears expired token from memory and `localStorage`.
  - Displays toast notification: `"Your session has expired. Please sign in to continue."`
  - Saves current URL and active booking ID in redirect state.
  - Navigates to `/login?returnUrl={currentUrl}`.
- **API Interaction**: Any protected API returning `401 Unauthorized`.
- **Authoritative Backend State**: Token rejected by Spring Security filter.
- **Successful Outcome**: User re-authenticates via OTP and is returned to their previous location.
- **Failure Outcomes**: None.
- **Navigation Outcome**: Redirects to `/login`.

---

### Flow V: Network Failure
- **Actor**: Customer
- **Starting Condition**: Browser goes offline or Wi-Fi drops.
- **User Actions**: Any interaction.
- **Frontend Behavior**:
  - Listens to `window.addEventListener('offline')`.
  - Displays top banner: `"You are currently offline. Check your internet connection."`
  - Disables mutation buttons (`Proceed to Book`, `Pay`) to prevent stalled network queues.
  - Automatically hides banner and triggers background query re-validation when `window.addEventListener('online')` fires.
- **API Interaction**: Browser fetch fails with `NetworkError`.
- **Authoritative Backend State**: Unaffected.
- **Successful Outcome**: Clear visual feedback without UI freezing.
- **Failure Outcomes**: None.
- **Navigation Outcome**: Remains on current screen.

---

### Flow W: Idempotent Retry Behavior
- **Actor**: Customer encountering a network glitch during hold or payment.
- **Starting Condition**: Initial POST request timed out or returned HTTP 504 Gateway Timeout.
- **User Actions**: Clicks "Retry".
- **Frontend Behavior**:
  - Re-issues the exact same HTTP request using the **exact same `Idempotency-Key` header** generated for the original attempt.
  - Ensures backend deduplicates request rather than creating a second hold or double charge.
- **API Interaction**:
  - `POST /api/v1/shows/{showId}/holds` or `POST /api/v1/payments` with original `Idempotency-Key`.
- **Authoritative Backend State**: Backend looks up existing idempotency record and returns the previously processed result.
- **Successful Outcome**: Returns original created resource without duplicate allocation.
- **Failure Outcomes**: Original failed -> clean error rendered.
- **Navigation Outcome**: Proceeds to next step upon success.

---

### Flow X: Empty Search and Filter Results
- **Actor**: Customer searching for movies or showtimes.
- **Starting Condition**: Entering a query with no matches (e.g. title: "NonexistentFilm").
- **User Actions**: Types in search field or selects restrictive genre/date filters.
- **Frontend Behavior**:
  - Debounces input by 300ms before triggering API.
  - Renders empty state card:
    - Icon: `Film` with dashed circle.
    - Title: `"No Movies Found"`.
    - Description: `"We couldn't find any movies matching 'NonexistentFilm'. Try checking your spelling or adjusting filters."`
    - CTA: `"Clear Filters"`.
- **API Interaction**:
  - `GET /api/v1/movies?title=NonexistentFilm` -> returns `[]`
- **Authoritative Backend State**: Read-only query returns empty JSON array.
- **Successful Outcome**: User easily recovers by clearing filters.
- **Failure Outcomes**: None.
- **Navigation Outcome**: Remains on `/movies`.

---

### Flow Y: API and Server Errors (500, 502, 503, 504)
- **Actor**: Customer
- **Starting Condition**: A backend microservice or gateway experiences transient downtime or unhandled exception.
- **User Actions**: Any API-dependent action.
- **Frontend Behavior**:
  - Catches HTTP 5xx responses via centralized API error interceptor.
  - Maps to clean, non-technical customer message:
    - 502 / 503: `"Cinema services are temporarily updating. Please refresh in a moment."`
    - 504: `"The request timed out. Please verify your internet connection or check My Bookings."`
    - 500: `"An unexpected error occurred while processing your request. Please try again."`
  - Inline alert displays a primary **"Retry"** button.
- **API Interaction**: Any API returning HTTP 5xx.
- **Authoritative Backend State**: Unaffected or rolled back by transactional boundaries.
- **Successful Outcome**: Prevents blank screens; guides customer on recovery.
- **Failure Outcomes**: Persistent downtime continues to show informative banner.
- **Navigation Outcome**: Stays on current view.
#END
