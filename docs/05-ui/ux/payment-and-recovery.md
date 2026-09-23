#START
# UX Specification: Payment and Recovery

## 1. Overview and Core Invariants
The BookNGo payment experience is engineered for maximum financial clarity, prevention of duplicate charges, and seamless recovery from distributed failures, network drops, and provider timeouts.

### Core Payment Principles:
1. **Zero Client-Authoritative Pricing**: The frontend never calculates or passes the payable amount to `POST /api/v1/payments`. The backend Payment Service calls Booking Service internally to determine the exact, immutable payable amount.
2. **Strict Idempotency**: Every payment initiation sends a unique client-generated UUIDv4 `Idempotency-Key` header. Retries must strictly reuse this key to prevent duplicate bank charges.
3. **Grace Period Protection**: Initiating payment before the 5-minute hold expires grants a maximum **2-minute payment-processing grace period**.
4. **No Premature Confirmation**: The UI never displays "Booking Confirmed" until `Booking.status === 'CONFIRMED'`.
5. **Verified Simulation Values**: `POST /api/v1/payments` accepts `providerName`. The verified simulation values supported by the backend are strictly:
   - `SIM_SUCCESS` → exercises the successful payment lifecycle
   - `SIM_FAILURE` → exercises the payment failure path
   - `SIM_CANCELLED` → exercises customer cancellation
   - `SIM_UNKNOWN` → exercises the unknown state and reconciliation machine
   The frontend must not invent or submit unverified external provider strings.

---

## 2. Payment State Model & Customer Message Mapping

| Backend PaymentState | Technical Meaning | Customer-Facing Presentation | Available User Actions |
|---|---|---|---|
| **INITIATED** | Payment record created; contacting provider | Animated radar spinner: *"Contacting payment provider..."* | None (Screen locked to prevent navigation) |
| **PENDING** | Awaiting external provider response | Spinner with grace notice: *"Awaiting bank confirmation. Grace window active..."* | None |
| **SUCCESS** | Provider verified charge; booking confirmed | Celebratory green checkmark: *"Payment Successful! Booking confirmed."* | "View Digital Ticket" |
| **FAILURE** | Bank or provider declined payment | Red alert card: *"Payment Failed: Your payment could not be processed by your bank."* | "Retry Payment" (if hold valid) or "Back to Seat Selection" |
| **CANCELLED** | Customer or provider aborted checkout | Neutral alert: *"Payment Cancelled: You cancelled the payment transaction."* | "Select New Seats" |
| **TIMEOUT** | 2-minute processing grace elapsed | Amber warning: *"Payment Processing Timed Out: We are confirming the transaction with your bank."* | "Check Status Again" / "Go to My Bookings" |
| **UNKNOWN** | Provider response lost or ambiguous | Amber warning card: *"Payment Status Pending: To protect you from duplicate charges, we are reconciling the status."* | "Check Status" / "View My Bookings" |
| **REFUND_PENDING** | Reversal queued following cancellation/failure | Blue pill: *"Refund Pending: Your refund has been initiated."* | "Track Refund Status" |
| **REFUNDED** | Reversal confirmed by provider | Green pill: *"Refund Completed: Amount credited to original payment source."* | View receipt |

---

## 3. End-to-End Payment Flow & Transitions

```text
               /checkout/:bookingId
                        │
                        ▼
                 [Pay ₹Total CTA]
                        │
                        ▼  (Attaches Idempotency-Key)
               POST /api/v1/payments
                        │
                        ├──────────────────────────┐
                        ▼                          ▼
                 HTTP 201 Created           HTTP 409 Conflict
                        │                   (Hold Expired)
                        ▼                          │
         /checkout/:bookingId/processing           ▼
                        │                  Modal: "Hold Expired"
                        │                  Redirect to Seat Map
                        │
            Poll GET /payments/{id}/status (every 2s)
                        │
     ┌──────────────┬───┴──────────┬──────────────┐
     ▼              ▼              ▼              ▼
  SUCCESS        FAILURE       CANCELLED       UNKNOWN /
     │              │              │           TIMEOUT
     ▼              ▼              ▼              │
/confirmation   Failure Card  Return to Shows     ▼
     │          "Retry Pay"               Status Resolution
     ▼                                    Screen (Check Status)
  /ticket
```

---

## 4. Polling Strategy & Grace Period Management

### Polling Mechanics:
- When the customer arrives on `/checkout/:bookingId/processing`, TanStack Query begins polling `GET /api/v1/payments/{paymentId}/status` every **2,000 milliseconds**.
- Polling stops immediately upon receiving any terminal state: `SUCCESS`, `FAILURE`, `CANCELLED`, `TIMEOUT`, `UNKNOWN`.

### 2-Minute Payment Grace Window:
- Backend hold creation sets a normal 5-minute expiry (`normalExpiresAt`).
- When `POST /api/v1/payments` is accepted, the backend sets `paymentGraceExpiresAt` to 2 minutes from initiation.
- The UI replaces the 5-minute hold timer with a **Payment Grace Timer**:
  - Visual: Amber pill with pulsating clock icon.
  - Copy: `"Securing your seats: 01:54 remaining in payment grace window. Please do not close or refresh this tab."`

---

## 5. Failure and Edge-Case Recovery

### Scenario A: Immediate Payment Failure (Bank Decline)
- Provider returns immediate `FAILURE` (e.g., insufficient funds, incorrect OTP).
- The polling query returns `{ "status": "FAILURE" }`.
- **UI Behavior**:
  - Displays a clean error card:
    - Title: `"Payment Declined"`
    - Explanation: `"Your financial institution could not complete this transaction."`
  - If the original 5-minute hold is still within valid time:
    - Provides a **"Try Another Payment Method"** CTA button.
    - User clicks button, returns to `/checkout/:bookingId`, and selects another provider using a *new* idempotency key.
  - If the hold has expired:
    - Disables retry.
    - Provides **"Return to Seat Map"** button.

### Scenario B: Payment Cancellation by User
- If the customer explicitly clicks "Cancel Transaction" on a provider simulator or dismisses the checkout drawer:
  - Frontend records status `CANCELLED`.
  - Releases the server hold via `DELETE /api/v1/holds/{holdId}`.
  - Displays notice: `"Seats released. No charges were made."`
  - Navigates back to show selection.

### Scenario C: Network Disconnection During Payment Execution
- Customer submits payment, bank processes the charge, but the customer's internet connection drops before the browser receives the HTTP response.
- **Recovery Path**:
  1. The payment provider independently notifies the backend via webhook (`POST /api/v1/payments/provider/callback`).
  2. The Payment Service marks the payment `SUCCESS` and Booking Service confirms the booking (`CONFIRMED`).
  3. When customer regains connectivity and opens BookNGo, they navigate to "My Bookings" (`/history`).
  4. The booking is displayed with a green `CONFIRMED` badge.
  5. The customer clicks "View Ticket" and accesses their digital pass without needing support intervention.

### Scenario D: Payment UNKNOWN / Gateway Timeout
- Provider gateway encounters an internal timeout or does not deliver a webhook within the 2-minute grace window.
- The payment transitions to `UNKNOWN` or `TIMEOUT`.
- **UI Behavior**:
  - Displays an informative reconciliation banner:
    `"We are verifying your transaction with your bank. To protect you from duplicate charges, we are reconciling the status. If your account was debited, your booking will confirm automatically."`
  - Provides a **"Check Status"** button that invokes `GET /api/v1/payments/{paymentId}/status` on demand.
  - Directs the customer to `/history` to monitor the booking.

### Scenario E: Accidental Duplicate Submission Prevention
- **Physical Button Disabling**: The moment the user clicks "Pay", the button is disabled, the label changes to `"Submitting..."`, and a full-element overlay prevents subsequent clicks.
- **Navigation Lock**: A `beforeunload` listener warns the user if they attempt to close the tab or press browser Back during payment processing:
  `"Your payment is currently being processed. Leaving now may cause transaction interruption."`
- **Idempotency Guarantee**: If a network failure triggers an automated retry, the client reuses the exact same `Idempotency-Key` header. The backend Payment Service identifies the key in `payment_idempotency_records` and returns the existing attempt rather than initiating a duplicate charge.

---

## 6. Refund and Reversal Presentation
When a booking is cancelled or payment encounters a confirmation rollback:
- The Booking Details screen (`/bookings/:bookingId`) displays a dedicated **Refund Status Card**:
  - Refund Reference: `REF-98421` (from `refund_reversals.refund_reversal_id`).
  - Amount: `₹Total`.
  - Status Tag:
    - `REFUND_PENDING`: Amber tag: `"Refund Initiated • Processing with your bank"`.
    - `REFUNDED`: Emerald tag: `"Refund Completed • Credited to original payment method"`.
  - Non-Committal Timeline Notice:
    `"Refunds are credited back to your original source of payment according to standard banking timelines. Please check your bank statement."` *(Never promises an exact number of days unless backend contract specifies).*
#END
