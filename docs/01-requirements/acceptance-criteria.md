# Acceptance Criteria

These criteria define the most important externally observable behaviors of
BookNGo.

---

# AC-01 — Normal Booking

Given a customer has authenticated,

When the customer:

1. Selects a movie.
2. Selects a theatre.
3. Selects a show.
4. Selects available seats.
5. Successfully holds the seats.
6. Initiates payment.
7. Payment succeeds.
8. Booking confirmation succeeds.

Then:

- The booking becomes CONFIRMED.
- The seats become BOOKED.
- A digital ticket is generated.

---

# AC-02 — Same-Seat Concurrency

Given:

```text
Show = S1
Seat = A10

And multiple customers concurrently attempt to acquire A10,

Then:

Successful allocation <= 1
Double booking = 0

Only one request may successfully establish the seat allocation.

AC-03 — Different Shows May Use the Same Physical Seat

Given:

Screen 1
 └── Physical Seat A10

Show A → Screen 1
Show B → Screen 1

Then A10 may independently be booked for Show A and Show B.

The uniqueness boundary is:

(show_id, physical_seat_id)
AC-04 — Hold Expiration

Given a customer successfully holds A10,

When five minutes pass without payment initiation,

Then:

A10 → AVAILABLE

Another customer may subsequently acquire A10.

AC-05 — Payment Processing Grace

Given a customer successfully holds seats,

When payment is initiated before the five-minute hold expires,

Then the booking may enter:

PAYMENT_PENDING

The system may allow up to two additional minutes for payment processing.

The seats must not remain locked indefinitely.

AC-06 — Payment Initiated After Hold Expiry

Given the customer's five-minute hold has expired,

When the customer attempts to initiate payment,

Then:

The expired booking must not be confirmed.
The expired seats must not remain locked.
A valid subsequent customer may acquire the seats.
AC-07 — Payment Failure

Given:

A10 → HELD

When payment fails,

Then:

Booking ≠ CONFIRMED
A10 → AVAILABLE
AC-08 — Payment Cancellation

Given a customer cancels payment,

Then:

The booking must not become CONFIRMED.
The applicable held inventory must be released according to the booking
state rules.
AC-09 — Payment Unknown

Given payment reaches UNKNOWN,

Then:

The system must not blindly create another payment.
The payment transaction must remain identifiable.
Payment status must be recoverable.
The booking must not be falsely confirmed.
AC-10 — Payment Success but Confirmation Failure

Given payment succeeds,

When booking confirmation fails,

Then:

The payment result remains recorded.
The booking does not falsely appear CONFIRMED.
Recovery/refund/reversal processing is initiated.
The customer can determine the resulting status.
AC-11 — Real-Time Seat Update

Given User A successfully holds A10,

When User B is viewing the same show,

Then User B's seat map should receive the changed state without requiring a
manual refresh.

AC-12 — Client Disconnect

Given User A successfully holds A10,

When User A closes the browser or disconnects,

Then the system shall not permanently preserve the hold.

After the applicable expiry:

A10 → AVAILABLE
AC-13 — Duplicate Booking Request

Given the same logical booking request is submitted twice,

Then:

Request 1 → successful
Request 2 → no duplicate booking

The second request must be handled idempotently.

AC-14 — Duplicate Payment Request

Given the same logical payment operation is submitted twice,

Then the system must not create two unintended payment transactions.

AC-15 — Duplicate Payment Callback

Given the same payment callback is received more than once,

Then the system must process it idempotently.

Duplicate callbacks must not create duplicate state transitions or charges.

AC-16 — Cancellation Policy

For a cancellable show:

Cancellation permitted when deadline allows it

For a non-cancellable show:

Cancellation not permitted

The customer must be informed of the policy before booking.

AC-17 — Theatre Operator Isolation

Given Operator A manages Theatre A,

When Operator A attempts to modify Theatre B,

Then the operation must be rejected unless Operator A is authorized for
Theatre B.

AC-18 — JWT Protection

Given an authenticated endpoint,

When a request is sent without a valid JWT,

Then the request must be rejected.

A request with a valid authorized JWT must be allowed according to its role.

AC-19 — Concurrent Mixed Seats

Given multiple customers concurrently request overlapping sets of seats,

Then:

No physical seat is allocated to more than one successful booking for the
same show.
Non-conflicting seats may be allocated independently.
Requests must fail safely when one or more requested seats cannot be acquired.
AC-20 — Booking State Integrity

At no point may the system expose a confirmed seat allocation without a
corresponding valid confirmed booking.

AC-21 — Load Test

Under the defined load-test scenario with approximately 1,000 concurrent
users/booking requests:

The system remains operational.
Concurrent requests are processed.
Same-seat contention maintains the no-double-booking invariant.
The test records successful and rejected requests.
No duplicate confirmed show-seat allocations occur.
