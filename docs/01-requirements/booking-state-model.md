# Booking State Model

## 1. Seat Inventory State

A seat's authoritative state is maintained for a specific:

(show_id, physical_seat_id)

### State Flow

AVAILABLE
   │
   │ successful hold
   ▼
HELD
   │
   ├── hold expires ───────────────→ AVAILABLE
   │
   ├── payment fails ──────────────→ AVAILABLE
   │
   ├── payment cancelled ──────────→ AVAILABLE
   │
   ├── payment times out/unknown
   │        │
   │        └── recovery determines final outcome
   │
   └── successful booking confirmation
            ↓
          BOOKED

BOOKED
   │
   └── successful permitted cancellation
            ↓
        AVAILABLE

## 2. Booking Lifecycle

INITIATED
    │
    │ successful seat hold
    ▼
HELD
    │
    ├── 5-minute hold expires
    │        ↓
    │     EXPIRED
    │
    ├── customer cancels before payment
    │        ↓
    │     CANCELLED
    │
    └── payment initiated while hold valid
             ↓
       PAYMENT_PENDING
             │
             ├── payment success
             │       ↓
             │   CONFIRMED
             │
             ├── payment failure
             │       ↓
             │   PAYMENT_FAILED
             │
             ├── payment cancelled
             │       ↓
             │   PAYMENT_CANCELLED
             │
             └── payment timeout/unknown
                     ↓
              PAYMENT_UNKNOWN
CONFIRMED
    │
    ├── customer cancels after payment
    │        ↓
    │   CANCELLED
    │
    └── refund processing begins
             ↓
        REFUND_PENDING
             │
             └── refund completes successfully
                     ↓
                REFUNDED


## 3. Payment Processing Grace

When a booking enters PAYMENT_PENDING:

The original 5-minute hold has already been successfully created.
Payment must have been initiated before that hold expired.
A maximum 2-minute payment-processing grace period is allowed.
The system must not allow indefinite seat locking.
5-minute hold
     │
     └── payment initiated
              │
              ▼
       PAYMENT_PENDING
              │
        up to 2 minutes
              │
       ┌──────┼─────────┐
       ▼      ▼         ▼
    SUCCESS  FAILURE  TIMEOUT/
                         UNKNOWN
## Booking-Level States

A Booking aggregates multiple seats and payment outcomes.

### State Flow

INITIATED
    ↓
HELD
    ↓
PAYMENT_PENDING
    ↓
CONFIRMED
    ↓
TICKET_ISSUED

### Alternative Failure Paths

HELD ──────────→ EXPIRED
HELD ──────────→ CANCELLED

PAYMENT_PENDING → PAYMENT_FAILED
PAYMENT_PENDING → EXPIRED

CONFIRMED → CANCELLED
CONFIRMED → REFUND_PENDING
REFUND_PENDING → REFUNDED

This gives us enough states to handle the important failure scenarios without creating unnecessary complexity.
