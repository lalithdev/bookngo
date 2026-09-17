# Concurrency Strategy

## 1. Purpose

This document defines how BookNGo maintains correct seat allocation under high concurrency.

The primary concurrency requirement is:

> For a given show, a physical seat must never be successfully allocated to more than one booking.

The concurrency strategy applies primarily to the **Booking Service**, because Booking Service owns show-specific seat inventory, seat holds, and booking state.

The system must remain correct even when multiple users attempt to select and book the same seat at nearly the same time.

---

## 2. Concurrency-Sensitive Resource

BookNGo separates:

- Physical seats
- Show-specific seat inventory

A physical seat belongs permanently to a screen.

A show uses a particular screen, but each show has its own seat availability.

Therefore, the concurrency boundary is:

```text
(show_id, physical_seat_id)
````

Example:

```text
Show A + Seat A10
Show B + Seat A10
```

These are two independent inventory records.

A seat being booked for Show A must not affect the same physical seat for Show B.

---

## 3. Authoritative Seat State

The Booking Service database is the authoritative source for show-seat availability.

The frontend seat map is only a representation of the current state.

Possible show-seat states:

```text
AVAILABLE
HELD
BOOKED
```

The server is authoritative.

The UI timer, displayed seat state, cached data, or realtime notification must never be treated as proof that a seat can be booked.

A request may receive:

```text
Seat available
```

from the UI and still fail because another transaction acquired the seat before the server processed the request.

The server must therefore validate availability inside the transaction that performs the state transition.

---

## 4. Core Concurrency Invariant

For every:

```text
(show_id, physical_seat_id)
```

the system must ensure:

```text
At most one active allocation exists at a time.
```

More specifically:

```text
AVAILABLE → HELD
```

must be successful for at most one concurrent request.

Likewise:

```text
HELD → BOOKED
```

must only be successful for the booking that owns the valid hold.

No two users may obtain a successful booking confirmation for the same:

```text
show_id + physical_seat_id
```

---

## 5. Database as the Concurrency Authority

Application-level checks alone are insufficient.

The system must not rely on:

```text
if seat is available:
    mark seat as held
```

because two application instances can execute the check simultaneously.

Example race condition:

```text
User A → checks seat → AVAILABLE
User B → checks seat → AVAILABLE

User A → marks seat HELD
User B → marks seat HELD
```

Both requests could incorrectly succeed if the operation is not atomic.

Therefore, the Booking Service must use database-backed concurrency control.

PostgreSQL is the authoritative mechanism for protecting the critical seat-allocation operation.

---

## 6. Transaction Boundary for Seat Hold

Creating a hold must be performed as one atomic database transaction.

Conceptually:

```text
BEGIN TRANSACTION

1. Identify requested show-seat inventory records.
2. Validate that requested seats exist for the show.
3. Lock or atomically update the relevant inventory rows.
4. Verify that seats are AVAILABLE.
5. Create the hold/booking records.
6. Change the seats to HELD.
7. Commit.

COMMIT
```

If any requested seat cannot be acquired, the complete seat-hold operation fails.

The system must not partially hold a multi-seat request.

Example:

User requests:

```text
A1
A2
A3
```

If:

```text
A1 → AVAILABLE
A2 → AVAILABLE
A3 → HELD by another user
```

then the request must not hold A1 and A2 while leaving A3 unavailable.

The complete request fails.

---

## 7. Preferred Seat Allocation Mechanism

The implementation should use an atomic database update or row-level locking strategy.

The exact implementation may use PostgreSQL row-level locks such as:

```text
SELECT ... FOR UPDATE
```

or an equivalent conditional update.

The critical requirement is that the operation must prevent two concurrent transactions from successfully changing the same available seat into HELD.

A conditional update can conceptually follow:

```text
UPDATE show_seat_inventory
SET
    status = 'HELD',
    active_hold_id = :holdId,
    hold_expires_at = :expiry
WHERE
    show_id = :showId
    AND physical_seat_id = :seatId
    AND (
        status = 'AVAILABLE'
        OR (
            status = 'HELD'
            AND hold_expires_at < CURRENT_TIMESTAMP
        )
    );
```

This ensures that expired HELD seats can be reclaimed lazily during acquisition even if a background cleanup job has not yet run.

The application then verifies the number of affected rows.

For one requested seat:

```text
affected rows = 1
```

means the seat was successfully acquired.

```text
affected rows = 0
```

means another transaction already changed the seat state or the seat is otherwise unavailable.

For multiple seats, all required seats must be successfully acquired within the same transaction.

---

## 8. Row-Level Locking

Where row-level locking is used, the Booking Service locks only the relevant show-seat inventory records.

It must not lock the entire show or entire inventory table for a normal booking request.

Example:

```text
User A requests:
A1, A2

User B requests:
B1, B2
```

The system should allow these independent operations to proceed concurrently.

If both users request:

```text
A1
```

then the database must serialize access to that particular inventory record.

Conceptually:

```text
Transaction A
    ↓
Lock A1
    ↓
Acquire A1
    ↓
Commit
    ↓
Unlock

Transaction B
    ↓
Attempts A1
    ↓
Sees A1 unavailable
    ↓
Fails
```

This preserves concurrency while protecting the critical resource.

---

## 9. Multi-Seat Booking

A booking can contain multiple seats.

The system must acquire all requested seats atomically.

Example:

```text
Booking Request:
A1, A2, A3
```

Required result:

```text
A1 → HELD
A2 → HELD
A3 → HELD
```

or:

```text
A1 → unchanged
A2 → unchanged
A3 → unchanged
```

There must be no successful partial allocation.

### Lock Ordering

When multiple seat rows are locked, the implementation should acquire them in a deterministic order, such as:

```text
A1
A2
A3
```

rather than depending on request order.

This reduces the possibility of deadlocks when concurrent requests contain overlapping seat sets.

Example:

```text
Transaction A requests A1, A2
Transaction B requests A2, A1
```

Both should use the same canonical ordering:

```text
A1 → A2
```

---

## 10. Hold Creation

The exact logical hold acquisition workflow is:

```text
inventory acquisition
    ↓
booking(INITIATED)
    ↓
seat_hold(ACTIVE)
```

This workflow ensures full compatibility with `seat_holds.booking_id NOT NULL`. The `bookings` record is created in the `INITIATED` status during the hold creation transaction.

Once successfully created:

```text
AVAILABLE → HELD
```

The server records:

```text
hold_id
booking_id
user_id
show_id
physical_seat_id
hold_created_at
hold_expires_at
```

The server-side expiry timestamp is authoritative.

---

## 11. Hold Duration

The normal hold duration is:

```text
5 minutes
```

The frontend displays the remaining time to the customer.

However:

```text
Frontend timer ≠ authoritative timer
```

The server/database determines whether the hold is still valid.

If the client clock differs from the server clock, the server state takes precedence.

---

## 12. Hold Expiration

A HELD seat must eventually become AVAILABLE if the hold expires without successful progression to a valid booking/payment state.

Conceptually:

```text
HELD
  │
  │ 5-minute expiry
  ↓
AVAILABLE
```

Expired holds must not remain permanently locked.

Hold expiration may be implemented using a scheduled cleanup mechanism, such as a Spring scheduled task or another controlled background process.

The cleanup process must be safe to run concurrently with booking/payment operations.

It must only release a hold if the corresponding hold has actually expired and is still in a releasable state.

---

## 13. Expiry Race Condition

A critical race can occur around hold expiry.

Example:

```text
T1: Hold expires
T2: User attempts payment/confirmation
```

The system must not rely on the frontend timer to decide the result.

The Booking Service must validate the hold inside the relevant transaction.

Possible outcomes:

### Case A — Hold still valid

```text
Hold valid
→ payment/confirmation may continue
```

### Case B — Hold expired

```text
Hold expired
→ booking cannot be confirmed normally
```

The system must ensure that an expired hold cannot be converted into a confirmed booking merely because the client submitted a late request.

---

## 14. Payment Processing Grace Period

Normal seat hold duration:

```text
5 minutes
```

If payment is initiated while the hold is still valid, the booking may enter:

```text
PAYMENT_PENDING
```

and receive a bounded payment-processing grace period of:

```text
2 minutes
```

This prevents indefinite seat locking.

Conceptually:

```text
AVAILABLE
    ↓
HELD
    ↓
PAYMENT_PENDING
    ↓
CONFIRMED
```

or:

```text
PAYMENT_PENDING
    ↓
TIMEOUT / PAYMENT_UNKNOWN / PAYMENT_FAILED
    ↓
Seat recovery according to the booking/payment state
```

The exact implementation must ensure that a seat cannot remain locked indefinitely because of a payment request.

---

## 15. Payment and Seat Consistency

The Booking Service and Payment Service are separate services and therefore do not share a database transaction.

The system must not assume that a distributed ACID transaction exists across both services.

Instead, the workflow must explicitly handle intermediate states.

Example:

```text
Booking
   ↓
PAYMENT_PENDING
   ↓
Payment Service
   ↓
SUCCESS
   ↓
Booking confirmation
```

If payment succeeds but booking confirmation cannot complete, the system must enter a recovery path.

Conceptually:

```text
PAYMENT_SUCCESS
        ↓
Confirmation failure
        ↓
REFUND_PENDING / reversal
```

The booking must not falsely appear CONFIRMED without valid booking state.

---

## 16. Payment Unknown

A network failure may occur after payment initiation or after the provider processes the payment but before the client receives the response.

Example:

```text
Client
  ↓
Payment Service
  ↓
External Provider
  ↓
Payment succeeds
  X
Response lost
```

The client cannot safely assume:

```text
Payment failed
```

because the payment may actually have succeeded.

Therefore, the system must support:

```text
PAYMENT_UNKNOWN
```

or an equivalent timeout/reconciliation state.

A retry must not automatically create another payment attempt that could result in a duplicate charge.

Payment status must be reconciled before a new charge is initiated.

---

## 17. Idempotency

Critical operations must be idempotent.

At minimum, idempotency is required for:

* Booking creation
* Payment initiation
* Payment callbacks/webhooks
* Booking confirmation where applicable

A client-generated or server-generated idempotency key can identify a logical operation.

Example:

```text
Idempotency-Key: abc123
```

If the same request is retried:

```text
Request 1 → abc123
Request 2 → abc123
```

the system must not create two independent effects.

The second request should return the result of the existing operation or an equivalent safe response.

---

## 18. Duplicate Booking Requests

Example:

```text
User clicks "Book"
       ↓
Request reaches server
       ↓
Booking succeeds
       X
Response lost
       ↓
User clicks "Book" again
```

The second request must not create another booking for the same logical operation.

The Booking Service should use an idempotency record and appropriate uniqueness constraints to detect duplicate operations.

---

## 19. Duplicate Payment Requests

Payment is especially sensitive.

Example:

```text
Pay
 ↓
Payment succeeds
 ↓
Response lost
 ↓
User presses Pay again
```

The second request must not blindly create another charge.

Payment Service must identify the existing payment attempt using the idempotency key and return the previously known state.

If the first attempt is UNKNOWN, the system should reconcile its status before initiating another charge.

---

## 20. Database Uniqueness Constraints

Application logic must be backed by database constraints.

The Booking database should enforce uniqueness for the logical relationships that must never be duplicated.

Important uniqueness boundaries include:

```text
(show_id, physical_seat_id)
```

for one show-seat inventory record.

Idempotency records should also have a unique key appropriate to their operation scope.

These constraints provide a final protection layer if application-level concurrency handling encounters an unexpected race.

---

## 21. Booking Confirmation

A booking can only become:

```text
CONFIRMED
```

when the required conditions are satisfied.

At minimum:

```text
Valid booking/hold
+
Valid payment success
+
Seats still associated with the booking
```

The confirmation operation must verify the current state rather than trusting an earlier response.

The transition must be atomic within the Booking Service database.

---

## 22. Cancellation and Concurrency

Cancellation may race with other booking/payment operations.

Example:

```text
Customer A → attempts cancellation
Customer B → attempts another state-changing operation
```

The Booking Service must validate the current booking state and cancellation policy before changing state.

Cancellation must not create an invalid transition such as:

```text
CANCELLED → CONFIRMED
```

without an explicitly supported recovery process.

Cancellation execution belongs to Booking Service.

Refund execution/status belongs to Payment Service.

---

## 23. Service Instance Concurrency

Booking Service may eventually run multiple instances:

```text
Booking Instance 1
Booking Instance 2
Booking Instance 3
```

All instances may receive requests for the same show and seat.

Correctness must therefore not depend on:

* In-memory locks
* Java `synchronized`
* Local JVM variables
* A single server instance

For example, this is insufficient:

```java
synchronized (seat) {
    // booking logic
}
```

because the lock only exists inside one JVM.

Another Booking Service instance would not respect it.

The database must provide the cross-instance concurrency boundary.

---

## 24. Horizontal Scaling

The Booking Service should remain stateless at the application layer wherever possible.

Multiple instances can therefore process requests concurrently:

```text
                    ┌─ Booking Instance 1
API Gateway ────────┼─ Booking Instance 2
                    └─ Booking Instance 3
                              │
                              ↓
                         PostgreSQL
```

The database controls the critical seat allocation race.

Application instances do not need to know which instance processed a previous request.

---

## 25. Realtime Seat Updates

Realtime updates improve visibility but do not provide correctness.

Example:

```text
User A sees A10 AVAILABLE
User B acquires A10
User A still has old UI state
User A attempts A10
```

The server must reject the stale request safely.

Realtime communication may be used to notify clients that:

```text
A10 → HELD
A10 → BOOKED
A10 → AVAILABLE
```

However:

```text
Realtime event ≠ authorization to book
```

The Booking Service database remains authoritative.

---

## 26. Concurrent Same-Seat Scenario

Example:

```text
Show: M1-7PM
Seat: A10

User A ─────┐
User B ─────┼──→ Booking Service
User C ─────┤
User D ─────┘
```

All users request:

```text
(show=M1-7PM, seat=A10)
```

Expected behavior:

```text
One request
    ↓
A10 → HELD

Remaining requests
    ↓
A10 unavailable
    ↓
Rejected
```

Exactly one request can successfully acquire the seat.

There must be:

```text
1 successful allocation
0 double bookings
```

---

## 27. Concurrent Overlapping Seats

Example:

```text
User A → A1, A2
User B → A2, A3
```

The database must resolve the conflict for A2.

Possible result:

```text
User A:
A1 → HELD
A2 → HELD

User B:
A2 → unavailable
A3 → not held
```

Because a multi-seat request must be atomic, User B's complete request fails rather than partially acquiring A3.

The exact winner is determined by transaction ordering and database concurrency, not by application preference.

---

## 28. Failure During Hold Creation

If the application or database fails during hold creation:

```text
Request
 ↓
Partial database work
 ↓
Failure
```

the transaction must roll back incomplete changes.

The desired property is:

```text
No partial successful hold
```

After recovery, the inventory must remain in a valid state.

---

## 29. Failure During Payment

If payment fails:

```text
PAYMENT_PENDING
      ↓
PAYMENT_FAILED
      ↓
Booking cannot become CONFIRMED
```

The associated seat hold must eventually be released according to the booking/payment recovery rules.

A payment failure must never leave the seat permanently locked.

---

## 30. Payment Success + Booking Failure

Exceptional flow:

```text
Payment succeeds
       ↓
Booking confirmation fails
       ↓
Booking is NOT marked CONFIRMED
       ↓
Payment enters refund/reversal handling
       ↓
REFUND_PENDING
       ↓
REFUNDED
```

The customer must receive a recoverable status/message.

The system must preserve enough state to determine:

```text
What happened?
What payment was created?
Which booking was affected?
What recovery action is required?
```

No exact refund completion time should be promised by the system unless guaranteed by the payment provider.

---

## 31. Server Failure During Booking

"Server down" is treated as a failure/recovery scenario rather than a promise of zero downtime.

The system must avoid leaving inconsistent seat state.

Persistent database state should allow the service to recover information about:

* Active holds
* Expired holds
* Bookings
* Payment attempts
* Idempotency operations

After restart, background recovery/expiry processing can reconcile stale states.

---

## 32. Hold Cleanup Safety

Hold cleanup must itself be concurrency-safe.

Example:

```text
Cleanup process → releases expired hold
Payment process → confirms booking
```

These operations could happen near the same time.

The implementation must ensure that an expired hold cannot be released after it has legitimately transitioned into a protected booking/payment state.

The state transition and expiry validation must therefore be performed atomically.

---

## 33. Transaction Isolation

PostgreSQL transaction isolation and row-level locking should be selected based on the actual operation.

The design should avoid unnecessarily using the strongest possible isolation level for every request because that can reduce throughput.

The critical requirement is:

```text
Concurrent allocation of the same show-seat must be serialized safely.
```

Normal non-conflicting bookings should remain capable of executing concurrently.

The final implementation should be validated using integration tests and concurrency tests rather than relying only on theoretical guarantees.

---

## 34. Deadlock Avoidance

The system should minimize deadlock risk by:

1. Locking only required rows.
2. Acquiring multiple seat locks in deterministic order.
3. Keeping transactions short.
4. Avoiding unnecessary service calls while database locks are held.
5. Avoiding long-running external payment operations inside the seat-allocation transaction.

In particular:

```text
Database transaction
        ↓
Acquire seats
        ↓
Persist state
        ↓
Commit
```

should not hold database locks while waiting for an external payment provider.

Payment processing must be handled as a separate workflow.

---

## 35. External Service Calls and Transactions

The Booking Service must not keep a PostgreSQL transaction open while waiting for:

```text
Payment Service
OTP Service
External payment provider
```

For example, this design should be avoided:

```text
BEGIN
 ↓
Lock seats
 ↓
Call Payment Provider
 ↓
Wait 5 seconds
 ↓
Receive response
 ↓
Commit
```

This would unnecessarily hold database locks and reduce concurrency.

Instead:

```text
Transaction
 ↓
Create/validate booking state
 ↓
Commit

Payment workflow
 ↓
Payment Service
 ↓
Provider
 ↓
Result

Transaction
 ↓
Confirm/recover booking
 ↓
Commit
```

---

## 36. Concurrency Testing Strategy

Concurrency testing is a mandatory part of the project because high-concurrency booking is the core business requirement.

Testing must include simultaneous requests against the same show and overlapping seats.

### Test Scenario 1 — Same Seat

Setup:

```text
1 show
1 seat
100+ concurrent requests
```

Expected:

```text
Successful allocations = 1
Double bookings = 0
```

### Test Scenario 2 — Same Seat, Many Users

Example:

```text
1000 concurrent clients
same show
same target seat
```

Expected:

```text
1 successful allocation
999 rejected/unavailable requests
0 double bookings
```

The exact number of clients can be scaled according to available local/online test infrastructure.

---

## 37. 1000-User Load Test

The project requirement targets approximately:

```text
1000 concurrent users/booking requests
```

under defined load-test conditions.

The primary correctness test should use:

```text
1000 concurrent clients
same show
same/overlapping seats
simultaneous booking attempts
```

The test should measure:

* Successful allocations
* Rejected allocations
* Duplicate bookings
* Failed requests
* Error responses
* Response time
* Database behavior
* Service stability

The test does not require 1000 real payment transactions.

Payment can be simulated for the academic project.

---

## 38. Mixed-Seat Load Test

A realistic load test should also include multiple seat requests.

Example:

```text
1000 concurrent clients
        ↓
Same show
        ↓
Different and overlapping seat combinations
```

The test should verify:

```text
No duplicate show-seat allocation
No invalid booking confirmation
No permanently locked seats caused by failed requests
```

This tests both contention and normal parallel booking behavior.

---

## 39. Load Test Success Criteria

A concurrency test is considered successful when:

```text
For every (show_id, physical_seat_id):

Successful allocations ≤ 1
```

and:

```text
Double bookings = 0
```

Additionally:

```text
Expired holds eventually become AVAILABLE
Failed payments do not become CONFIRMED
Duplicate requests do not create duplicate effects
```

The system should remain operational under the defined test load.

---

## 40. Concurrency Test Data Verification

After the load test, the database should be checked directly for invariant violations.

For each show-seat:

```text
(show_id, physical_seat_id)
```

verify that there is no state indicating multiple simultaneous successful ownership records.

Booking records should also be checked for:

```text
duplicate booking
duplicate booking-seat association
invalid confirmation
```

The test result should include database-level verification, not only HTTP response counts.

---

## 41. Recovery Testing

Concurrency testing must also include failure scenarios.

Examples:

### Test A — Client disconnects after hold

```text
Create hold
↓
Client disconnects
↓
Hold expires
↓
Seat becomes AVAILABLE
```

### Test B — Payment response lost

```text
Payment initiated
↓
Response lost
↓
Payment state becomes UNKNOWN
↓
Reconciliation/status check
```

### Test C — Booking request retry

```text
Request succeeds
↓
Response lost
↓
Same idempotency key retried
↓
No duplicate booking
```

### Test D — Payment succeeds but confirmation fails

```text
Payment SUCCESS
↓
Booking confirmation failure
↓
Refund/reversal workflow
```

---

## 42. Concurrency Responsibility by Service

### Booking Service

Owns:

* Show-seat inventory
* Seat hold
* Booking state
* Seat allocation
* Booking confirmation
* Booking cancellation execution
* Ticket issuance

It is the primary concurrency-sensitive service.

### Show Service

Owns:

* Show definition
* Show schedule
* Pricing
* Cancellation policy

It does not own the runtime seat-allocation state.

### Theatre Service

Owns:

* Theatre
* Screen
* Physical seats

It does not decide whether a physical seat is available for a particular show.

### Payment Service

Owns:

* Payment attempts
* Payment state
* Payment idempotency
* Refund/reversal
* Payment reconciliation

It does not directly allocate seats.

---

## 43. Architectural Principle

The key concurrency principle for BookNGo is:

```text
Physical Seat
      ↓
owned by Theatre Service

Show
      ↓
owned by Show Service

Show-specific Seat Inventory
      ↓
owned by Booking Service

Seat Allocation
      ↓
protected by Booking DB transaction/concurrency control
```

This separation allows the system to scale while keeping the most important invariant in one authoritative service.

---

## 44. Final Concurrency Model

The intended model is:

```text
                API Gateway
                     │
                     ▼
              Booking Service
                     │
             ┌───────┴───────┐
             │               │
             ▼               ▼
        Application      PostgreSQL
          Instances       Inventory
             │               │
             │        Row-level/atomic
             │          concurrency
             │               │
             └───────────────┘
```

The application layer coordinates the booking workflow.

PostgreSQL protects the critical inventory state.

The database, rather than an individual application instance, is the final authority for concurrent seat allocation.

---

## 45. Architectural Decisions Frozen

The following decisions are considered fixed for implementation:

| Decision                                    | Final Choice                                 |
| ------------------------------------------- | -------------------------------------------- |
| Concurrency boundary                        | `(show_id, physical_seat_id)`                |
| Inventory owner                             | Booking Service                              |
| Physical seat owner                         | Theatre Service                              |
| Concurrency authority                       | Booking PostgreSQL database                  |
| Application-level distributed locking       | Not required for core allocation             |
| Seat allocation                             | Atomic DB operation / row-level locking      |
| Multi-seat booking                          | Atomic all-or-nothing allocation             |
| Normal hold duration                        | 5 minutes                                    |
| Payment grace                               | 2 minutes after payment initiation           |
| Hold timer authority                        | Server                                       |
| Realtime updates                            | Visibility only                              |
| Idempotency                                 | Required for critical operations             |
| Duplicate payment protection                | Payment Service + idempotency                |
| Cross-service DB transaction                | Not used                                     |
| External calls inside seat-lock transaction | Avoided                                      |
| Same-seat concurrent allocation             | At most one success                          |
| Double booking                              | Must be impossible under correct operation   |
| Hold recovery                               | Persistent state + expiry processing         |
| Load target                                 | Approximately 1000 concurrent users/requests |
| Payment during load test                    | Can be simulated                             |

---

## 46. Implementation Constraint

AI agents and developers must not replace the above concurrency model with a simpler implementation merely for convenience.

In particular, implementation must not:

* Use frontend seat state as the source of truth.
* Use only in-memory locks.
* Use only Java `synchronized` blocks for concurrency.
* Allow partial multi-seat holds.
* Hold database locks while waiting for payment providers.
* Confirm bookings without valid payment state.
* Create duplicate payments on retries.
* Depend on a single Booking Service instance.
* Permanently lock seats when a client disconnects.
* Treat realtime events as authoritative inventory state.

Any change to these architectural decisions must be explicitly reviewed before implementation.
