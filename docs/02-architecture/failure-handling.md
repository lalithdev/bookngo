
# Failure Handling Strategy

## 1. Purpose

This document defines how BookNGo handles service failures, database failures, communication failures, payment failures, client disconnects, and partial failures.

The objective is to ensure that failures do not result in:

- Double booking
- Permanently locked seats
- False booking confirmation
- Duplicate payment
- Lost booking state
- Inconsistent service ownership
- Unrecoverable payment state

The system should favor safe failure and recoverable state over attempting to hide failures from the user.

---

## 2. Failure Handling Principles

BookNGo follows these principles:

1. Persist important state before relying on it.
2. Never use frontend state as the authoritative source.
3. Never use in-memory state as the only source for critical booking information.
4. Keep database transactions short.
5. Avoid distributed transactions across microservices.
6. Use idempotency for retryable critical operations.
7. Use bounded timeouts for synchronous service calls.
8. Retry only operations that are safe to retry.
9. Treat payment uncertainty differently from payment failure.
10. Ensure expired holds can eventually be released.
11. Preserve enough state to recover after service restart.
12. Fail closed for security-sensitive operations.
13. Never confirm a booking based only on a client assertion.

---

## 3. Failure Categories

BookNGo considers the following major failure categories:

```text
Client Failure
Service Failure
Database Failure
Network Failure
Payment Provider Failure
Concurrent Request Failure
Timeout
Partial Failure
Deployment/Restart Failure

Each category requires a different recovery strategy.

4. Client Disconnect

A customer may close the browser, lose connectivity, or navigate away after creating a hold.

Example:

User
 ↓
Create Hold
 ↓
Seat = HELD
 ↓
Browser disconnects

The system must not depend on receiving a "release seat" request from the client.

The hold remains controlled by the server-side expiry mechanism.

After the configured hold duration:

HELD → AVAILABLE

provided that the booking has not legitimately progressed into a protected payment/confirmation state.

5. Booking Service Failure

If Booking Service becomes temporarily unavailable:

Client
 ↓
API Gateway
 ↓
Booking Service
 X

the Gateway should return an appropriate service-unavailable response.

The system must not create a booking based on a frontend assumption that the request succeeded.

If the client did not receive a response, the user may safely retry using the same idempotency key where supported.

6. Booking Service Restart

Booking Service must be recoverable after restart.

Critical information must be persisted in PostgreSQL, including:

Show-seat inventory
Hold information
Booking state
Booking-seat relationships
Idempotency records
Ticket information
Relevant timestamps

The service must not depend on memory for determining whether a seat is currently booked.

After restart, the service can reconstruct the required operational state from the database.

7. Database Failure

If the Booking database becomes unavailable:

Booking Service
      ↓
PostgreSQL
      X

the service must not attempt unsafe seat allocation using local memory.

Booking operations requiring authoritative inventory state should fail safely.

The system should return an appropriate error rather than claiming that a seat was successfully booked.

8. Database Transaction Failure

If a database transaction fails:

BEGIN
 ↓
Seat allocation
 ↓
Booking update
 ↓
Database failure
 ↓
ROLLBACK

the incomplete transaction must not be treated as successful.

The application should return a failure response.

The database transaction provides atomicity for changes within the Booking Service.

9. Service-to-Service Communication Failure

BookNGo uses synchronous REST communication for the initial architecture.

Example:

Booking Service
      ↓
Show Service
      X

The calling service must use bounded connection/read timeouts.

A service must not wait indefinitely for another service.

If a dependency is unavailable, the calling service should return a controlled failure or use a previously defined fallback where safe.

10. Retry Policy

Retries must be used carefully.

Safe candidates generally include operations that are:

Read-only
Idempotent
Explicitly designed for retry

Retries should not blindly be applied to operations that may create financial or booking side effects.

For example:

GET movie

may be retried safely.

But:

POST payment

must use idempotency protection before retrying.

11. Retry Limits

Retries should be bounded.

The system should not perform:

Retry forever

because this can amplify an outage.

A bounded retry strategy should use:

Limited attempts
+
Timeout
+
Controlled backoff

The exact values are implementation/configuration decisions and should be validated during integration testing.

12. Circular Failure Prevention

Synchronous service dependencies must not create circular request chains.

Avoid patterns such as:

Booking → Show
Show → Booking
Booking → Theatre
Theatre → Booking

because one unavailable service could cascade into another.

Service responsibilities defined in service-boundaries.md should be preserved during implementation.

13. API Gateway Failure

The API Gateway is the primary entry point for frontend traffic.

If Gateway is unavailable:

Frontend
   ↓
Gateway
   X
Services

the frontend cannot reach backend services through the normal route.

The individual services may still be running, but the public application path is unavailable.

This is an infrastructure availability issue and should be handled through deployment/restart mechanisms.

14. Eureka Failure

Eureka provides service discovery.

If Eureka becomes temporarily unavailable, already-running services may continue operating depending on their locally cached service information and client configuration.

However, new discovery operations or service registration may be affected.

BookNGo should not make Eureka itself part of the critical database transaction for seat allocation.

Seat correctness must remain independent of service discovery.

15. Payment Service Failure

If Payment Service is unavailable during payment initiation:

Booking
 ↓
Payment Service
 X

the booking must not become:

CONFIRMED

without valid payment success.

The booking should remain in an appropriate pending/failure/recovery state and the associated seats must not remain locked indefinitely.

16. External Payment Provider Failure

Payment Service isolates the rest of BookNGo from the external payment provider.

Possible provider outcomes include:

SUCCESS
FAILURE
CANCELLED
TIMEOUT
UNKNOWN

The system must preserve the distinction between these states.

A timeout does not automatically prove that payment failed.

17. Payment Timeout

Example:

Payment Service
      ↓
Provider
      ↓
No response within allowed time

The payment attempt should enter a timeout or unresolved state according to the configured workflow.

The booking must not automatically become CONFIRMED.

If the provider can later report the transaction status, reconciliation should determine the final state.

18. Payment Unknown

Payment uncertainty is different from a confirmed failure.

Example:

Payment request
 ↓
Provider processes payment
 ↓
Network failure
 ↓
Client receives no response

The correct state may be:

PAYMENT_UNKNOWN

rather than:

PAYMENT_FAILED

The system must not immediately charge the customer again.

A reconciliation/status operation should determine whether the original payment succeeded.

19. Duplicate Payment Prevention

If the client retries after a timeout or lost response:

Pay
 ↓
Response lost
 ↓
Pay again

Payment Service must use idempotency.

The same logical payment request must resolve to the existing payment attempt rather than blindly creating another charge.

20. Payment Success but Booking Confirmation Failure

Exceptional failure:

Payment
 ↓
SUCCESS
 ↓
Booking confirmation
 X

The system must not mark the booking as confirmed merely because payment succeeded.

Instead:

Payment SUCCESS
        ↓
Confirmation failure
        ↓
Recovery
        ↓
REFUND_PENDING / reversal

The customer should be informed that payment processing succeeded but booking confirmation encountered a failure.

The exact refund completion time depends on the payment provider and must not be fabricated by the application.

21. Booking Confirmation Failure Recovery

The system should persist enough information to recover:

booking_id
payment_id
user_id
show_id
seat references
payment status
booking status
timestamps

This allows a reconciliation process or administrative recovery workflow to determine what happened.

22. Hold Expiration Failure

The expiry mechanism itself may temporarily fail.

Example:

Hold expires
 ↓
Cleanup process unavailable
 ↓
Seat remains HELD temporarily

The system must ensure that expired holds can be identified from persistent timestamps.

After the expiry mechanism recovers, it can process expired holds.

The hold must not rely on a volatile in-memory timer.

23. Expiry and Payment Race

A hold may expire while payment processing is occurring.

The system must resolve this using persisted state and transactional validation.

Possible sequence:

HELD
 ↓
PAYMENT_PENDING
 ↓
Payment result

The booking state determines whether the payment result can still produce a valid booking.

If the booking can no longer be confirmed, the payment must enter the appropriate recovery/reconciliation flow.

24. Partial Network Failure

A request may succeed on the server while the response fails to reach the client.

Example:

Client
 ↓
Booking Service
 ↓
Booking succeeds
 X
Response lost

The client must not assume:

Booking failed

and blindly create a new booking.

The user should be able to retrieve booking status using the existing booking/idempotency information.

25. Idempotent Recovery

For operations that may have completed before a response was lost:

Client retries
 ↓
Same idempotency key
 ↓
Existing operation found
 ↓
Existing result returned

This prevents duplicate effects.

This mechanism is especially important for:

Booking creation
Payment initiation
Payment callbacks
Confirmation-related operations
26. Duplicate Payment Callback

Payment providers may potentially deliver the same callback more than once.

Example:

Payment callback
Payment callback
Payment callback

Payment Service must process callbacks idempotently.

Repeated callbacks for the same payment event must not create:

Multiple confirmations
Multiple refunds
Multiple booking transitions
27. Invalid State Transition

The system must reject invalid transitions.

Examples:

CANCELLED → CONFIRMED
PAYMENT_FAILED → CONFIRMED
EXPIRED → CONFIRMED

unless an explicitly defined recovery operation supports that transition.

State changes must be validated against the current persisted state.

28. Security Failure

Authentication and authorization failures must fail closed.

Examples:

Missing JWT
Invalid JWT
Expired JWT
Insufficient role
Invalid OTP

must not result in access to protected operations.

Sensitive operations such as booking and payment initiation must require valid authentication.

29. OTP Failure

OTP delivery or verification may fail.

Possible outcomes:

OTP delivery timeout
Invalid OTP
Expired OTP
Too many attempts
Provider unavailable

The User Service must enforce OTP validity and attempt limits.

The failure must not create an authenticated session.

Review 1 may use a simulated OTP mechanism.

30. Theatre/Show Dependency Failure

A booking request may require information owned by Show or Theatre Service.

If required information cannot be obtained safely, Booking Service should not create an invalid booking.

For example, if the show cannot be verified:

Booking request
 ↓
Show validation
 X

the booking should fail safely.

The system must not invent show or seat metadata.

31. Data Ownership During Failure

Each service remains responsible for recovering its own data.

For example:

User Service → User database
Movie Service → Movie database
Theatre Service → Theatre database
Show Service → Show database
Booking Service → Booking database
Payment Service → Payment database

No service should repair another service's database directly.

Recovery should happen through service APIs, controlled events, or administrative processes.

32. Observability Requirements

Failures should be diagnosable.

Services should log relevant operational information such as:

Request/correlation ID
Service name
Operation
Entity/booking ID where appropriate
Result/state transition
Error category
Timestamp
Dependency failure

Sensitive information such as passwords, OTP values, payment credentials, and secrets must not be logged.

33. Correlation ID

A request should carry a correlation/request identifier through the system.

Conceptually:

Frontend
   │
   │ X-Request-ID
   ▼
Gateway
   │
   ├── User Service
   ├── Show Service
   ├── Booking Service
   └── Payment Service

This allows a booking failure to be traced across multiple services.

34. Failure Response Design

The API should distinguish between categories such as:

Validation failure
Authentication failure
Authorization failure
Resource unavailable
Conflict
Dependency failure
Timeout
Internal failure

The frontend can then provide meaningful messages instead of displaying a generic failure for every situation.

35. Safe Seat Failure Rule

The most important failure rule is:

If the system cannot prove that a seat was successfully acquired,
the system must not claim that the seat was acquired.

Likewise:

If the system cannot prove that a booking was successfully confirmed,
the system must not claim that the booking was confirmed.

This principle prevents false success.

36. No Permanent Seat Lock

Under all supported failure scenarios, seats must eventually be recoverable.

Examples:

Client disconnect
Payment failure
Payment timeout
Booking failure
Service restart
Expiry worker restart
Network interruption

The system must retain sufficient state to identify and release seats that are no longer legitimately held.

37. Failure Recovery Matrix
Failure	Expected Behavior
Client disconnect	Hold eventually expires
Booking service unavailable	Booking request fails safely
Booking DB unavailable	No authoritative booking allocation
Transaction failure	Transaction rolls back
Show service unavailable	Booking requiring validation fails safely
Payment service unavailable	Booking not confirmed
Payment failure	Booking not confirmed; hold eventually released
Payment timeout	Enter timeout/recovery state
Payment unknown	Reconcile before retrying charge
Duplicate payment request	Idempotent response
Duplicate callback	Idempotent processing
Lost booking response	Retry/status lookup using idempotency
Booking confirmation failure	Recovery/refund workflow
Eureka unavailable	Discovery may degrade; booking correctness unaffected
Gateway unavailable	Public request path unavailable
Expiry worker unavailable	Persistent expiry timestamps allow later recovery
Invalid JWT	Request rejected
Invalid OTP	Authentication rejected
38. Review 1 Failure Demonstrations

The following scenarios should be demonstrated during integration testing where practical:

Demo 1 — Same Seat Concurrency
Multiple users
 ↓
Same show
 ↓
Same seat
 ↓
One successful allocation
Demo 2 — Hold Expiration
Hold created
 ↓
Expiry
 ↓
Seat becomes AVAILABLE
Demo 3 — Duplicate Request
Same booking request
 ↓
Same idempotency key
 ↓
No duplicate booking
Demo 4 — Payment Failure
Payment failure
 ↓
No CONFIRMED booking
 ↓
Seat recovery
Demo 5 — Lost Response Simulation
Successful operation
 ↓
Simulated response loss
 ↓
Retry
 ↓
No duplicate effect
39. Architecture Constraint

AI agents implementing BookNGo must preserve the failure-handling behavior defined here.

Agents must not simplify the system by:

Removing idempotency
Treating payment timeout as guaranteed payment failure
Storing critical hold state only in memory
Confirming bookings after arbitrary retries
Calling external payment providers inside long database transactions
Allowing failed operations to permanently lock seats
Using frontend state as authoritative
Directly modifying another service's database

Any change to these behaviors must be explicitly reviewed before implementation.

