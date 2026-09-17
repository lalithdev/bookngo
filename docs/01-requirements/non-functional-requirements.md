# Non-Functional Requirements

## NFR-01 — Concurrency

The platform shall support a target workload of approximately:

**1,000 concurrent users/booking requests**

under defined load-test conditions.

The target shall be evaluated through controlled testing rather than assumed
from theoretical capacity.

## NFR-02 — Seat Consistency

Seat allocation shall maintain strong consistency for the critical booking
operation.

For a particular:

(show_id, physical_seat_id)

the system shall not permit two successful confirmed allocations.

## NFR-03 — Concurrency Safety

Concurrent attempts to acquire the same show-seat shall resolve safely.

The system shall guarantee:

- At most one successful active allocation at a time.
- No double booking.
- No conflicting authoritative inventory states.

## NFR-04 — Failure Safety

Temporary service, network, or client failures shall not permanently lock
seat inventory.

Expired holds must eventually be released.

## NFR-05 — Payment Recovery

Payment state must remain recoverable when:

- The customer loses network connectivity.
- The payment response is delayed.
- The booking response is lost.
- A payment callback is duplicated.
- A downstream confirmation operation fails.

## NFR-06 — Idempotency

Repeated logical operations must not create unintended duplicate effects.

This applies to:

- Booking creation.
- Payment initiation.
- Payment callbacks/webhooks.
- Recovery operations where applicable.

## NFR-07 — Performance

Normal browsing operations should provide interactive response times.

Critical booking operations should provide a clear success or failure result
rather than leaving the customer indefinitely uncertain.

Exact latency targets shall be established through testing instead of
inventing unsupported numerical guarantees.

## NFR-08 — Security

The system shall:

- Protect authenticated endpoints.
- Validate JWT credentials.
- Secure sensitive credentials.
- Protect OTP operations against abuse.
- Avoid exposing sensitive information.
- Restrict theatre operations by role.
- Restrict administrator operations by role.

## NFR-09 — Scalability

The architecture should allow high-load functionality, especially booking
and seat inventory, to scale independently from lower-load functionality.

## NFR-10 — Service Isolation

Failure of a non-critical service should not unnecessarily corrupt booking
inventory.

Critical failures shall fail safely.

## NFR-11 — Observability

The system should provide logs sufficient to diagnose:

- Booking attempts.
- Seat holds.
- Hold expiry.
- Payment events.
- Payment failures.
- Booking failures.
- Concurrent conflicts.
- Authentication failures.
- Service communication failures.

## NFR-12 — Real-Time Visibility

Seat-state changes should normally propagate to connected clients within a few
seconds.

Real-time propagation is not the source of truth.

Server-side inventory state remains authoritative.

## NFR-13 — Maintainability

Each business service should maintain clear ownership of its domain data.

Services should not directly modify another service's database.

## NFR-14 — Deployment

The demonstrable application should be accessible online where feasible.

The target total project cost is:

**₹0**

Free-tier infrastructure and simulated external integrations may be used.

## NFR-15 — Testability

The system shall support testing of:

- Normal booking.
- Concurrent seat contention.
- Hold expiry.
- Payment failure.
- Payment timeout/unknown state.
- Duplicate requests.
- Client disconnect.
- Booking confirmation failure.
- Cancellation.
- Authentication.