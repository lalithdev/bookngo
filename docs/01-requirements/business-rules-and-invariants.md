# Booking State Model

This document defines the logical state transitions that must be respected by
the implementation.

The exact database representation will be defined later.

---

# 1. Show-Seat Inventory State

Inventory is maintained for:

(show_id, physical_seat_id)

## Primary States

```text
AVAILABLE
   │
   │ successful hold
   ▼
HELD
   │
   ├──────── hold expires ────────→ AVAILABLE
   │
   ├──────── payment fails ───────→ AVAILABLE
   │
   ├──────── payment cancelled ───→ AVAILABLE
   │
   └──────── successful booking ──→ BOOKED
                                      │
                                      │ permitted cancellation
                                      ▼
                                   AVAILABLE

The frontend may display these states but does not control the authoritative
transition.

2. Booking State
INITIATED
    │
    │ successful seat hold
    ▼
HELD
    │
    ├──────── hold expires ────────→ EXPIRED
    │
    ├──────── customer cancels ────→ CANCELLED
    │
    └──────── payment initiated
                     │
                     ▼
              PAYMENT_PENDING
                     │
          ┌──────────┼──────────────┐
          │          │              │
          ▼          ▼              ▼
       SUCCESS    FAILURE       TIMEOUT/
          │          │           UNKNOWN
          │          ▼              │
          │     PAYMENT_FAILED      │
          │                         │
          ▼                         ▼
      CONFIRMED                PAYMENT_UNKNOWN
          │
          ▼
     TICKET_ISSUED
3. Payment Processing Window

Normal hold:

HOLD CREATED
    │
    └── 5 minutes

If payment is initiated before the hold expires:

HOLD
 │
 │ payment initiated
 ▼
PAYMENT_PENDING
 │
 └── maximum 2-minute payment-processing grace

The additional grace period must not create indefinite seat locking.

4. Payment Success

Normal successful flow:

HELD
  ↓
PAYMENT_PENDING
  ↓
PAYMENT_SUCCESS
  ↓
CONFIRMED
  ↓
TICKET_ISSUED
5. Payment Failure
HELD
  ↓
PAYMENT_PENDING
  ↓
PAYMENT_FAILED
  ↓
Booking not confirmed
  ↓
Held inventory released
6. Payment Timeout / Unknown
HELD
  ↓
PAYMENT_PENDING
  ↓
TIMEOUT / UNKNOWN

The system must not blindly retry payment.

The payment transaction remains recoverable through status checking or
reconciliation.

The final inventory transition must be determined safely according to the
defined processing window and recovery rules.

7. Payment Success but Booking Confirmation Failure
PAYMENT_SUCCESS
      ↓
CONFIRMATION_FAILURE
      ↓
REFUND_PENDING
      ↓
REFUNDED

The booking must not falsely appear CONFIRMED.

For Review 1, this process may be simulated.

8. Confirmed Booking Cancellation
CONFIRMED
    │
    │ cancellation permitted
    ▼
CANCELLED
    │
    ▼
REFUND_PENDING
    │
    ▼
REFUNDED

Cancellation is permitted only when the show's configured cancellation policy
allows it.

9. State Transition Principles
A frontend countdown never determines authoritative expiry.
A client disconnect never permanently preserves a hold.
A failed payment never confirms a booking.
UNKNOWN payment must not automatically trigger another charge.
A successful payment must remain recoverable.
A booking retry must not create a duplicate booking.
A payment retry must not create a duplicate charge.
A seat can only be confirmed once for a particular show.
