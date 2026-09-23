#START
# UX Specification: Booking History and Digital Ticket Pass

## 1. Overview and Core Invariants
This document specifies the user experience for reviewing past and upcoming bookings, accessing digital cinema ticket passes, and managing booking cancellations.

### Immutable Principles:
1. **Verified Confirmation Only**: A booking is never presented as `CONFIRMED` or `TICKET_ISSUED` unless the backend `Booking.status` explicitly returns that status.
2. **Immutable Ticket Snapshot**: Once a ticket is issued, its `historicalSnapshot` represents an immutable record of the film, theatre, showtime, seat labels, and transaction amount at the time of purchase.
3. **Policy-Enforced Cancellation**: Cancellation actions are governed strictly by the show's authoritative `cancellation-policy` and showtime deadline. The frontend provides transparent policy terms and disables cancellation when deadlines elapse.

---

## 2. Booking History Screen (`/history`)

### Layout and Visual Hierarchy:
- **Header**: Page title `"My Bookings"` with total booking counter.
- **Filter Tabs**:
  - `All` (Default)
  - `Upcoming` (Shows scheduled in the future with status `CONFIRMED` or `TICKET_ISSUED`)
  - `Past / Completed`
  - `Cancelled`
- **Booking Card Item**:
  - Left: Movie poster thumbnail (`w-20 aspect-[2/3] rounded-lg`).
  - Center:
    - Movie title in bold (`text-lg text-gray-50`).
    - Theatre name, screen name, and city (`text-sm text-gray-400`).
    - Show date and start time with calendar icon (`text-sm text-gray-300 font-medium`).
    - Allocated seats formatted as tags: `[A10] [A11]`.
  - Right:
    - Status badge:
      - `CONFIRMED`: Green pill (`bg-emerald-500/10 text-emerald-400 border border-emerald-500/30`).
      - `CANCELLED`: Rose pill (`bg-rose-500/10 text-rose-400 border border-rose-500/20`).
      - `EXPIRED` / `PAYMENT_FAILED`: Muted grey pill.
    - Total amount: `₹700`.
    - Primary CTA: **"View Ticket"** (for `CONFIRMED` bookings) or **"View Details"**.

### Loading, Empty, and Error States:
- **Loading**: Stack of 3 card skeletons with pulsing rectangles.
- **Empty State**:
  - Icon: `Ticket` outline.
  - Title: `"No Bookings Yet"`.
  - Description: `"You haven't booked any movie tickets yet. Check out what's currently showing in theatres!"`
  - CTA Button: `"Explore Movies"` (navigates to `/movies`).
- **Error State**:
  - Alert card: `"Unable to load your bookings. [Retry]"`

### Data Coordination Architecture:
- `GET /api/v1/bookings/me` returns the user's `Booking[]` array.
- Because `Booking` holds `showId` rather than embedded movie/theatre metadata, the frontend coordinates queries via TanStack Query (`booking` -> `show` -> `movie` and `theatre`) to populate card titles and showtimes from cached or lazy-fetched entities without modifying backend contracts.

---

## 3. Booking Details Screen (`/bookings/:bookingId`)

### Comprehensive Order Breakdown:
1. **Header Banner**:
   - Booking reference ID (e.g., `BKNG-A9281F`) and booking timestamp (`23 Sep 2026, 07:15 PM`).
   - Prominent status indicator badge.
2. **Movie & Show Section**:
   - Poster image, title, language, format (e.g., `IMAX 2D`), and runtime.
   - Cinema name, full street address, and screen name (`Screen 3 - Audi 1`).
   - Screening start time and anticipated end time.
3. **Seat Allocation Section**:
   - Table detailing each booked seat:
     - Physical Seat Label (e.g., `Row A Seat 10`)
     - Category (`Premium`)
     - Unit Price (`₹350.00`)
4. **Financial Breakdown**:
   - Ticket Subtotal: `₹700.00`
   - Convenience Fee & GST: `₹68.00`
   - Total Paid: `₹768.00` (Currency: `INR`)
   - Payment Reference ID: `PAY-48912`
   - Payment Status: `SUCCESS`
5. **Action Bar**:
   - Primary: **"View Digital Ticket"** (Green button).
   - Secondary (Conditional): **"Cancel Booking"** (Outline red button).

---

## 4. Digital Ticket Pass Screen (`/bookings/:bookingId/ticket`)

The ticket pass is styled to evoke the tactile, premium aesthetic of an authentic cinema ticket stub:

```text
┌─────────────────────────────────────────────────────────────┬───────────────────────┐
│  BOOKNGO CINEMA PASS                                        │  SCAN AT ENTRANCE     │
│                                                             │                       │
│  INCEPTION                                                  │     ┌───────────┐     │
│  English • IMAX 2D • 2h 28m                                 │     │  QR CODE  │     │
│                                                             │     │   SCAN    │     │
│  PVR Cinemas, Phoenix Palladium, Screen 1                   │     └───────────┘     │
│  Wednesday, 23 Sep 2026 • 07:30 PM                          │                       │
│  - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  │  TICKET CODE:         │
│  SEATS:  ROW A:  10,  11        CATEGORY: PREMIUM           │  BNG-9842-XC71        │
│  TOTAL AMOUNT PAID: ₹768.00     STATUS:   ISSUED            │                       │
└─────────────────────────────────────────────────────────────┴───────────────────────┘
```

### Ticket Details & Functional Actions:
- **Visual Design**:
  - Two-tone container (`bg-slate-900` body with dark perforated dashed line separator).
  - High-contrast scannable QR code preview (visually encoding `ticketCode` for gate admission, without cryptographic claims).
  - Monospace ticket code for manual entry at cinema box office: `BNG-9842-XC71`.
- **Customer Actions**:
  - **"Print / Save Ticket"**: Invokes native print stylesheet that strips navigation headers and renders a clean, black-and-white printable gate pass.
  - **"Share Ticket"**: Copies clean text summary and link to clipboard.
  - **"Back to My Bookings"**: Navigates to `/history`.
- **Brightness Tip**:
  - Mobile prompt: `"Tip: Turn up screen brightness to help the gate scanner read your QR code quickly."`

---

## 5. Cancellation UX & Policy Enforcement

### Cancellation Eligibility Rules:
1. Cancellation is permitted **ONLY IF**:
   - `Booking.status === 'CONFIRMED'`.
   - The show's `cancellation-policy` has `policyType === 'CANCELLABLE'`.
   - The current time is before the `cancellationDeadline` (e.g., 2 hours before show start).
2. If the policy is `NON_CANCELLABLE` or the deadline has elapsed:
   - The "Cancel Booking" button is disabled or hidden.
   - An informative tooltip displays: `"Cancellation is not available for this show as per cinema policy."`

### Cancellation Modal Flow:
```text
Customer clicks "Cancel Booking"
              │
              ▼
   Displays Cancellation Confirmation Modal:
   - Title: "Cancel Ticket Booking?"
   - Warning: "Are you sure you want to cancel your tickets for Inception?"
   - Policy Note: "Full refund of ₹768.00 will be initiated to your original payment method."
   - Action 1: [Confirm Cancellation] (Red CTA)
   - Action 2: [Keep My Booking] (Slate button)
              │
              ▼ Customer clicks "Confirm Cancellation"
   POST /api/v1/bookings/{bookingId}/cancel
              │
      ┌───────┴────────┐
      ▼                ▼
HTTP 200 OK        HTTP 409 Conflict
(Cancelled)        (Deadline Passed)
      │                │
      │                ▼
      │      Toast: "Cancellation deadline has passed."
      ▼      Modal closes; button disabled.
1. Modal closes.
2. Booking status updates to CANCELLED.
3. Ticket status updates to CANCELLED (QR disabled).
4. Refund banner appears:
   "Refund Initiated: ₹768.00 is being processed."
```

---

## 6. Cancelled Booking Presentation
When viewing a cancelled booking:
- Top status badge renders in bold red: `CANCELLED`.
- Digital ticket button is disabled with notice: `"Ticket Invalidated upon Cancellation"`.
- Dedicated **Refund Tracking Card** renders:
  - Header: `"Refund Information"`
  - Amount: `₹768.00`
  - Refund Status: `REFUND_PENDING` (or `REFUNDED`)
  - Notice: `"Refunds are processed by your bank and typically appear on your statement within standard banking timeframes."`
#END
