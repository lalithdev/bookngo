#START
# UX Specification: Customer Booking Flow

## 1. Journey Overview
This document specifies the complete, sequential user experience for a customer booking a movie ticket on BookNGo. It covers every screen transition, loading state, visual feedback cue, failure mitigation, duplicate-action prevention, and mobile-specific behavior across the 9 core stages:

```text
Home (Discovery)
      ↓
Movie Details
      ↓
Show & Theatre Selection
      ↓
Seat Selection (Local Draft)
      ↓
Proceed to Book (Auth Gate / Hold Creation)
      ↓
Hold & Booking Review (Checkout)
      ↓
Payment Processing (Polling & Grace Window)
      ↓
Booking Confirmation
      ↓
Digital Ticket Pass
```

---

## 2. Step-by-Step Experience Specification

### Step 1: Home (Discovery)
- **What the User Sees**:
  - Top navigation with BookNGo brand logo, city selector dropdown (e.g., "Mumbai"), search input, and "Sign In" button.
  - Featured movie hero carousel with high-resolution cinematic artwork.
  - "Now Showing" movie grid displaying movie cards with posters, titles, certification badges, languages, and genre tags.
- **What the User Can Click**:
  - City dropdown to change location context.
  - Movie cards or "Book Now" buttons on posters.
  - Search input to type film titles.
- **While Requests are Running**:
  - Movie card skeletons with subtle pulse animations indicate loading without page jumping.
- **When Requests Fail**:
  - Inline error banner: "Unable to load movies. [Retry]".
- **Duplicate Prevention**: Debounced search input (300ms).
- **Mobile Differences**:
  - Hero carousel is touch-swipeable.
  - Movie cards collapse to single-column or 2-column swipeable rows.

---

### Step 2: Movie Details
- **What the User Sees**:
  - Immersive movie hero banner with trailer preview, poster, title, duration (e.g., "2h 48m"), release date, genres, and synopsis.
  - Prominent red primary CTA: **"Book Tickets"**.
- **What the User Can Click**:
  - "Book Tickets" CTA button.
  - "Watch Trailer" modal trigger.
  - Breadcrumbs to return to movie catalog.
- **Navigation Transition**:
  - Clicking "Book Tickets" scrolls or smoothly navigates to `/movies/:movieId/shows`.
- **Mobile Differences**:
  - "Book Tickets" docks as a fixed bottom bar (`h-16 bg-slate-900 border-t border-slate-800 flex items-center px-4 justify-between`) so it is always accessible regardless of scroll depth.

---

### Step 3: Show & Theatre Selection
- **What the User Sees**:
  - Horizontal calendar strip showing 7 selectable dates with day of the week and date number (e.g., "WED 24", "THU 25").
  - Format filter chips: "All", "2D", "3D", "IMAX".
  - List of theatres showing the movie, displaying theatre name, address, and available showtimes as clickable pills.
  - Each showtime displays format tag and cancellation badge ("Free Cancellation").
- **What the User Can Click**:
  - Date chips to switch day.
  - Format filters.
  - Showtime pill (e.g., "07:30 PM").
- **While Requests are Running**:
  - Showtime pills show subtle loading skeletons when switching dates.
- **When Requests Fail**:
  - Empty state displays: "No shows available for selected date. Check tomorrow."
- **Navigation Transition**:
  - Clicking a showtime pill immediately navigates to `/shows/:showId/seats`.

---

### Step 4: Seat Selection (Local Draft)
- **What the User Sees**:
  - Header with movie title, theatre name, screen name, and showtime.
  - Visual cinema screen arc at the top with "SCREEN THIS WAY".
  - Color-coded physical seat layout (VIP, Premium, Regular) organized by rows (A, B, C...) and seat numbers (1, 2, 3...).
  - Seat Legend: Available (outlined), Selected (solid green), Held by Others (striped amber), Booked (muted dark slate).
  - Floating/docked bottom summary bar with selected seat list, seat count, running total price, and "Proceed to Book" button.
- **What the User Can Click**:
  - Any `AVAILABLE` seat icon to toggle selection.
  - Zoom controls (`[+]`, `[-]`, `[Reset]`).
  - "Proceed to Book" CTA button (disabled when 0 seats selected).
- **Selection Constraints**:
  - Selecting a seat marks it `SELECTED` in local UI state only.
  - Maximum 6 seats allowed. Clicking a 7th seat triggers a warning toast: "Maximum 6 seats allowed per booking."
- **Concurrency & Live Inventory**:
  - The seat inventory query runs in the background every 5 seconds.
  - If another user acquires a seat that the current user has NOT selected, that seat silently turns amber or dark grey.
  - If another user acquires a seat that the current user HAS selected, the UI immediately shows a conflict toast: "Seat {label} is no longer available" and deselects it.
- **Mobile Differences**:
  - Pinch-to-zoom and two-finger drag enabled on the SVG seat map.
  - Selected seat summary is docked to the bottom with an expandable upward drawer.

---

### Step 5: Proceed to Book (Authentication Gate & Hold Creation)
- **What Happens When User Clicks "Proceed to Book"**:
  1. **Authentication Check**:
     - If user is NOT logged in: The frontend stores the pending selection (`showId`, `selectedSeatIds`) in `sessionStorage` and navigates to `/login?returnUrl=/shows/:showId/seats`.
     - User completes Phone OTP verification.
     - On successful login, user is returned to `/shows/:showId/seats` where their selection is automatically restored.
  2. **Hold Acquisition Request**:
     - If user IS logged in: The button changes to disabled with a loading spinner: `"Reserving your seats..."`.
     - Frontend generates a unique `Idempotency-Key` UUID.
     - Dispatches `POST /api/v1/shows/{showId}/holds` with the selected seat IDs.
- **When Hold Succeeds (`201 Created`)**:
  - The server returns `SeatHoldResponse` containing `holdId`, `bookingId`, `normalExpiresAt` (5 minutes from now), and `totalAmount`.
  - Frontend navigates immediately to `/checkout/:bookingId`.
- **When Hold Fails (`409 Conflict`)**:
  - A modal/toast alerts: "One or more of your selected seats were just taken by another moviegoer. Please choose alternative seats."
  - Inventory refreshes instantly to display authoritative availability.
  - Button returns to active state so user can pick other seats.

---

### Step 6: Hold & Booking Review (Checkout)
- **What the User Sees**:
  - Top alert banner with live countdown timer:
    `"Seats held for 04:52. Complete payment before the timer expires."`
  - Order summary card:
    - Movie title, poster thumbnail, theatre name, screen name, show date and time.
    - Selected seats with category badges (e.g., `A10, A11 (Premium)`).
    - Price breakdown: Ticket Base Price, Convenience Fee/Taxes, Total Payable Amount.
    - Cancellation policy notice: "Cancellable up to 2 hours before showtime".
  - Payment simulation selector: "Standard Payment (`SIM_SUCCESS`)", with test simulation options (`SIM_FAILURE`, `SIM_CANCELLED`, `SIM_UNKNOWN`).
  - Primary button: **"Pay ₹Total"**.
  - Secondary button: **"Cancel & Release Seats"**.
- **Timer Warnings**:
  - When timer reaches `< 60 seconds`: Banner pulses red, font changes to urgent bold.
  - When timer reaches `00:00`: Modal appears: `"Your hold has expired. Seats have been released."` with button `"Choose Seats Again"`.
- **Duplicate Click Prevention**:
  - Clicking "Pay" disables the button, attaches a spinner (`"Contacting payment provider..."`), and disables back navigation.

---

### Step 7: Payment Execution and Processing
- **What Happens When User Clicks "Pay"**:
  - Frontend sends `POST /api/v1/payments` with `{ bookingId, providerName: "SIM_SUCCESS" }` (or chosen simulation test) and a unique `Idempotency-Key`. The frontend never sends `amount` or `currency`; payable amount is authoritative on the backend.
  - Server applies 2-minute payment grace period to the booking and hold (`paymentGraceExpiresAt`).
  - Navigates to `/checkout/:bookingId/processing?paymentId={paymentId}`.
- **What the User Sees on Processing Screen**:
  - Animated radar pulsing spinner and cinema icon.
  - Status text: `"Processing your payment securely with bank..."`
  - Explicit warning: `"Please do not refresh, close, or navigate away from this page."`
  - Sub-timer indicating the 2-minute grace window.
- **Background Action**:
  - Frontend polls `GET /api/v1/payments/{paymentId}/status` every 2 seconds.
- **Resolution Outcomes**:
  - **Success**: Status changes to `SUCCESS`. Polling stops. Seamless transition to Step 8.
  - **Failure**: Status changes to `FAILURE`. Polling stops. Renders error message: "Payment was declined by your bank." Displays "Retry Payment" (if hold still valid) or "Return to Movies".
  - **Unknown / Timeout**: If grace period elapses without resolution, displays: "Transaction status pending. We are verifying with your bank. Check My Bookings in a few minutes."

---

### Step 8: Booking Confirmation
- **What the User Sees**:
  - Celebratory green confirmation animation.
  - Headline: `"Booking Confirmed!"`
  - Booking ID reference (e.g., `BKNG-7F89A`).
  - Confirmation notice: `"Your tickets are confirmed and ready in My Bookings."`
  - Condensed summary of movie, showtime, and seats.
  - Prominent primary CTA button: **"View Digital Ticket"**.
  - Secondary action: "Back to Home".
- **Navigation Transition**:
  - Clicking "View Digital Ticket" navigates to `/bookings/:bookingId/ticket`.

---

### Step 9: Digital Ticket Pass
- **What the User Sees**:
  - Realistic cinema ticket pass with perforated dashed edge and high contrast.
  - Movie title, poster, cinema name, auditorium/screen number, show date, and start time.
  - Scannable QR code visual representation (visually encoding `ticketCode` for gate admission, without cryptographic claims).
  - Alphanumeric `ticketCode` displayed in bold monospace text.
  - Allocated seat numbers prominently featured (`Row A: 10, 11`).
  - Total amount paid and verified status badge (`ISSUED`).
- **What the User Can Click**:
  - "Download / Print Ticket" button (triggers native `window.print()` formatted for ticket slip).
  - "Go to My Bookings" button.
- **Mobile Differences**:
  - Screen brightness suggestion tip: "Turn up screen brightness when scanning at the cinema gate."
#END
