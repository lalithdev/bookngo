# Functional Requirements

## 1. Movie and Discovery Requirements

### FR-01 — Movie Discovery

The system shall display movies available for booking.

### FR-02 — Location Selection

The system shall allow customers to select a city or supported location.

The system may support browser-based location detection as a convenience.

Manual location selection shall remain available if browser location access
is unavailable or denied.

### FR-03 — Show Discovery

The system shall allow customers to find shows using:

- Movie
- Location
- Date
- Theatre
- Language
- Format
- Show time

### FR-04 — Theatre Information

The system shall display theatres offering the selected movie/show.

The customer shall be able to view:

- Theatre name
- Location
- Show timing
- Language
- Format
- Cancellation availability

---

# 2. Seat and Inventory Requirements

### FR-05 — Seat Map

The system shall display the seating layout for a selected show.

Each show-seat shall have an authoritative state such as:

- AVAILABLE
- HELD
- BOOKED

The displayed state represents the current known server-side inventory.

### FR-06 — Maximum Seats

A customer shall be able to include a maximum of six seats in one booking
attempt.

### FR-07 — Server-Side Seat Hold

After the customer confirms the seat selection and requests booking, the
system shall attempt to create a server-side temporary hold.

Frontend seat selection alone shall not create a hold.

### FR-08 — Hold Duration

A successfully created seat hold shall be valid for five minutes.

### FR-09 — Hold Expiration

When the five-minute hold expires without payment initiation, the system
shall release the held seats.

### FR-10 — Held Seat Protection

A seat that is currently held for a particular show shall not be successfully
held by another customer.

The authoritative identity is:

(show_id, physical_seat_id)

---

# 3. Concurrency Requirements

### FR-11 — Concurrent Requests

The system shall support approximately 1,000 concurrent
users/booking requests under defined load-test conditions.

### FR-12 — Same-Seat Contention

When multiple customers concurrently attempt to acquire the same physical seat
for the same show:

At most one request shall successfully acquire the seat.

Example:

```text
100 concurrent requests
        │
        ├── Show S1 + Seat A10
        ├── Show S1 + Seat A10
        ├── Show S1 + Seat A10
        ├── Show S1 + Seat A10
        └── ...
                │
                ▼
        At most ONE success
        Remaining requests rejected
FR-13 — No Double Booking

The system shall never have two confirmed bookings containing the same
physical seat for the same show.

This is a hard business invariant.

4. Real-Time Requirements
FR-14 — Seat State Updates

When a seat changes state:

AVAILABLE → HELD
HELD → AVAILABLE
HELD → BOOKED
BOOKED → AVAILABLE where permitted by cancellation

connected customers viewing the same show should receive the updated state
automatically.

FR-15 — Real-Time Propagation

Seat-state changes should normally propagate to connected clients within a few
seconds.

The system shall not promise an exact latency before performance testing
establishes a supported target.

Real-time updates are a visibility mechanism.

The server-side inventory operation remains authoritative.

5. Authentication Requirements
FR-16 — Phone and OTP Authentication

The customer shall be able to authenticate using:

Phone Number
      ↓
OTP
      ↓
Verification
      ↓
Authenticated Session
FR-17 — OTP Validity

An OTP shall have a limited validity period.

Initial project value:

5 minutes

FR-18 — OTP Attempt Protection

The system shall limit repeated OTP verification attempts.

FR-19 — Authenticated API Protection

Authenticated API operations shall require valid authentication credentials.

JWT shall be used for authenticated API access.

6. Booking Requirements
FR-20 — Booking Data

A booking shall contain at minimum:

Customer
Show
Seats
Amount
Booking Status
Created Time
FR-21 — Booking Amount

The system shall calculate the booking amount using the applicable show
pricing and selected seat categories.

FR-22 — Confirmation Condition

A booking shall not become CONFIRMED until the required payment condition
has been successfully satisfied.

7. Payment Requirements
FR-23 — Payment Initiation

The system shall support payment initiation for a booking while the
associated seat hold remains valid.

FR-24 — Payment Outcomes

The system shall handle:

SUCCESS
FAILURE
CANCELLED
TIMEOUT
UNKNOWN
FR-25 — Failed Payment

A failed or cancelled payment shall not create a confirmed booking.

FR-26 — Payment Processing Grace

If payment is initiated before the five-minute seat hold expires, the booking
may remain PAYMENT_PENDING for a maximum additional payment-processing grace
period of two minutes.

The system shall not allow indefinite seat locking.

FR-27 — Payment and Booking Idempotency

The system shall prevent accidental duplicate processing when a customer
retries the same logical booking or payment operation.

Duplicate payment callbacks/webhooks shall also be processed idempotently.

FR-28 — Payment Unknown Recovery

When payment reaches TIMEOUT or UNKNOWN:

The system shall preserve the payment state.
The system shall provide a recoverable payment status.
The system shall not blindly initiate another charge.
A reconciliation/status mechanism shall be available where applicable.
FR-29 — Payment Success With Confirmation Failure

If payment succeeds but booking confirmation fails:

The payment result shall be preserved.
The booking shall not falsely appear CONFIRMED.
Recovery/refund/reversal processing shall be initiated.
The customer shall receive an appropriate status.

For Review 1, this process may be simulated.

8. Ticket Requirements
FR-30 — Digital Ticket

After successful booking confirmation, the system shall generate a digital
ticket.

The ticket should contain:

Ticket ID
Movie
Theatre
Screen
Date
Show Time
Seats
Customer
Amount
Booking Status

A QR code may be added later but is not required for Review 1.

9. Cancellation and Refund Requirements
FR-31 — Cancellation Policy

Each show shall have a cancellation policy:

CANCELLABLE
NON_CANCELLABLE
FR-32 — Policy Visibility

The customer shall be informed of the cancellation policy before booking.

FR-33 — Cancellation Eligibility

Cancellation shall only be allowed when permitted by the show's policy and
configured cancellation deadline.

FR-34 — Refund Handling

Refund handling shall be associated with the original payment transaction.

For Review 1, refund/reversal processing may be simulated.

10. Theatre Requirements

Theatre operators shall be able to perform the following operations for
assigned theatres.

FR-35 — Manage Theatre

Create, update, and view theatre information.

FR-36 — Manage Screens

Create, update, and manage screens belonging to a theatre.

FR-37 — Configure Physical Seats

Create and configure physical seats belonging to screens.

A physical seat belongs to a screen and is not independently owned by a show.

FR-38 — Create/Update Shows

Create and update scheduled shows.

Each show uses one screen.

FR-39 — Configure Pricing

Configure ticket prices using supported seat categories such as:

Regular
Premium
VIP
FR-40 — View Bookings

View bookings for assigned theatres/shows.

FR-41 — View Occupancy

View show occupancy based on booking/inventory information.

FR-42 — Configure Cancellation Policy

Configure whether a show is cancellable and, where applicable, its
cancellation deadline.

11. Failure and Recovery Requirements
FR-43 — Safe Service Failure

A temporary service failure shall not result in permanent seat locking.

FR-44 — Hold Recovery

Expired or incomplete holds shall eventually be released even if the
original client disconnects.

FR-45 — Booking State Integrity

A booking failure shall not leave the system in a state where a seat appears
confirmed without a valid confirmed booking.

FR-46 — Payment Recovery

A successful payment followed by client, network, or downstream confirmation
failure shall be recoverable without requiring the customer to pay again.

FR-47 — Duplicate Operation Protection

Retrying a booking/payment operation shall not unintentionally create:

Duplicate bookings.
Duplicate payment processing.
FR-48 — Physical Seat Across Shows

The same physical seat may be independently available or booked for different
shows using the same screen.

The uniqueness boundary is:

(show_id, physical_seat_id)

FR-49 — Server Authority

When frontend state differs from authoritative server inventory, the server
state shall determine whether the booking/hold operation succeeds.

A seat displayed as available may legitimately become unavailable before the
customer's request is processed.

12. Administrative Requirements
FR-50 — Movie Management

Platform administrators shall be able to manage movie information.

FR-51 — User and Theatre Account Management

Platform administrators shall be able to manage platform users and
theatre/operator accounts.

FR-52 — Platform Monitoring

Platform administrators shall be able to view basic operational information
required for platform oversight.