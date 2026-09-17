# BookNGo — Software Requirements Specification (SRS)

## 1. Document Information

| Field | Value |
|---|---|
| Product Name | BookNGo |
| Product Type | High-Concurrency Entertainment Ticket Booking & Seat Allocation Platform |
| Domain | Cinema / Entertainment Ticketing |
| Document | Software Requirements Specification |
| Version | 1.0 |
| Status | Draft for Review |
| Related Document | Product Requirements Document (PRD) |

---

# 2. Purpose

This Software Requirements Specification defines the functional, non-functional, behavioral, security, concurrency, reliability, and integration requirements for BookNGo.

The SRS translates the product-level goals defined in the PRD into software-level requirements that can be used to design, implement, test, and validate the platform.

The primary technical concern is the safe allocation of cinema seats when multiple customers concurrently attempt to book the same show and seats.

---

# 3. System Scope

BookNGo is a microservices-based entertainment ticket booking platform.

The system allows customers to:

- authenticate using phone number and OTP,
- discover movies,
- discover theatres and shows,
- view show-specific seat availability,
- select seats,
- temporarily hold seats,
- initiate payment,
- complete bookings,
- receive tickets,
- view booking history,
- and cancel eligible bookings.

The system also allows theatre operators to manage assigned theatres, screens, physical seats, shows, pricing, cancellation policies, bookings, and occupancy information.

Platform administrators manage platform-level movie and operator information.

---

# 4. System Context

The high-level interaction is:

```text
Customer / Theatre Operator / Admin
                |
                v
          Frontend Application
                |
                v
          API Gateway
                |
       +--------+--------+
       |        |        |
       v        v        v
    Business Microservices
       |
       v
  Service-owned Databases
````

Infrastructure components include:

* API Gateway
* Eureka Service Discovery

Business services currently identified are:

1. User Service
2. Movie Service
3. Theatre Service
4. Show Service
5. Booking Service
6. Payment Service

These boundaries must remain traceable to the requirements and will be validated before implementation.

---

# 5. Actors

## 5.1 Customer

A customer searches for movies and shows, selects seats, creates a hold, completes payment, receives tickets, views bookings, and performs eligible cancellations.

## 5.2 Theatre Operator

A theatre operator manages theatres assigned to them and their associated screens, physical seats, shows, pricing, cancellation policies, bookings, and occupancy.

## 5.3 Platform Administrator

An administrator performs platform-level management such as movie management, theatre-operator management, and basic oversight.

## 5.4 Payment Provider

An external payment provider represents the payment-processing boundary.

For Review-1, this may be simulated.

## 5.5 OTP Provider

An external OTP/SMS provider represents OTP delivery.

For Review-1, this may be simulated.

---

# 6. Functional Requirements

## 6.1 Authentication and User Management

### FR-AUTH-01 — OTP Request

The system shall allow a user to request an OTP using their phone number.

### FR-AUTH-02 — OTP Verification

The system shall allow a user to verify an OTP.

### FR-AUTH-03 — OTP Expiration

An OTP shall remain valid for a maximum of 5 minutes.

### FR-AUTH-04 — OTP Rate Limiting

The system shall limit excessive OTP requests and verification attempts.

### FR-AUTH-05 — JWT Issuance

Upon successful authentication, the system shall issue a JWT containing the required authenticated-user information.

### FR-AUTH-06 — Protected Operations

Protected APIs shall require a valid authentication token.

### FR-AUTH-07 — Role Authorization

The system shall support at least:

```text
CUSTOMER
THEATRE_OPERATOR
ADMIN
```

### FR-AUTH-08 — Resource Authorization

The system shall verify that users have permission to access or modify the requested resource.

A theatre operator shall only manage assigned theatres.

### FR-AUTH-09 — Role-Specific Authentication Credentials

The system shall authenticate users according to their assigned role:
- CUSTOMER users shall authenticate using Phone Number + OTP.
- THEATRE_OPERATOR users shall authenticate using Username + Password.
- ADMIN users shall authenticate using Username + Password.
All authenticated roles shall receive a signed JWT upon successful authentication.

---

# 7. Movie Requirements

### FR-MOV-01 — Movie Discovery

The system shall allow customers to retrieve available movies.

### FR-MOV-02 — Movie Details

The system shall provide movie details.

### FR-MOV-03 — Movie Management

Authorized administrators shall be able to create, update, and remove movies according to platform rules.

### FR-MOV-04 — Movie Search/Filtering

The system should support suitable movie discovery and filtering capabilities.

---

# 8. Theatre Requirements

### FR-THR-01 — Theatre Discovery

The system shall allow customers to discover theatres.

### FR-THR-02 — Theatre Details

The system shall provide theatre information relevant to show discovery and booking.

### FR-THR-03 — Screen Management

Authorized theatre operators shall be able to manage screens belonging to assigned theatres.

### FR-THR-04 — Physical Seat Management

Authorized theatre operators shall be able to manage physical seats belonging to screens.

### FR-THR-05 — Theatre Assignment

The system shall associate theatre operators with the theatres they are authorized to manage.

### FR-THR-06 — Location

The system shall support theatre discovery using location information.

Browser geolocation may be provided as a convenience feature.

Manual location selection shall remain possible when browser geolocation is unavailable or denied.

---

# 9. Show Requirements

### FR-SHOW-01 — Show Creation

Authorized theatre operators shall be able to create shows.

### FR-SHOW-02 — Show Details

The system shall provide show details including relevant movie, theatre, screen, date, and time information.

### FR-SHOW-03 — Show Discovery

Customers shall be able to discover shows using suitable criteria including:

* movie,
* location,
* date,
* theatre,
* language,
* format,
* show time.

### FR-SHOW-04 — Screen Association

Each show shall be associated with a screen.

### FR-SHOW-05 — Pricing

The system shall support show-specific pricing.

The initial implementation shall support simple seat categories such as:

```text
REGULAR
PREMIUM
VIP
```

### FR-SHOW-06 — Cancellation Policy

A show shall have a cancellation configuration.

The configuration shall support:

```text
CANCELLABLE
NON-CANCELLABLE
```

### FR-SHOW-07 — Cancellation Deadline

A cancellable show may have a configured cancellation deadline, such as 12 or 24 hours before show time.

---

# 10. Seat Inventory Requirements

## 10.1 Physical Seat

### FR-SEAT-01

A physical seat shall belong to a specific screen.

The relationship shall be:

```text
Theatre
   ↓
Screen
   ↓
Physical Seat
```

## 10.2 Show-Specific Inventory

### FR-SEAT-02

The system shall maintain availability for a physical seat separately for each show.

### FR-SEAT-03

The logical concurrency identity shall be:

```text
(show_id, physical_seat_id)
```

### FR-SEAT-04

The same physical seat may be available for one show while being booked for another show.

Example:

```text
Show A + Seat A10 = BOOKED
Show B + Seat A10 = AVAILABLE
```

---

# 11. Seat Availability Requirements

### FR-AVAIL-01 — Seat Map

The system shall provide a seat map for a selected show.

### FR-AVAIL-02 — Seat States

The system shall represent at least:

```text
AVAILABLE
HELD
BOOKED
```

### FR-AVAIL-03 — Server Validation

The server shall revalidate seat availability when a hold or booking operation is performed.

Frontend availability information shall not be treated as authoritative.

### FR-AVAIL-04 — Concurrent Acquisition

When multiple customers attempt to acquire the same seat for the same show concurrently, at most one operation shall successfully allocate that seat.

### FR-AVAIL-05 — No Double Booking

The system shall prevent the same `(show_id, physical_seat_id)` from being successfully booked by more than one active booking.

---

# 12. Seat Selection and Hold Requirements

### FR-HOLD-01 — Seat Selection

A customer shall be able to select one or more available seats.

### FR-HOLD-02 — Maximum Seats

A customer shall not be allowed to hold more than 6 seats in a single booking operation.

### FR-HOLD-03 — Hold Creation Trigger

Selecting a seat in the frontend shall not itself create a server-side hold.

The hold shall begin only when the customer proceeds to booking.

```text
Select Seats
     ↓
Continue / Proceed to Book
     ↓
Server Validates
     ↓
Hold Created
```

### FR-HOLD-04 — Hold Duration

A successfully created hold shall normally remain valid for 5 minutes.

### FR-HOLD-05 — Hold Expiration

An expired hold shall release its seats back to AVAILABLE.

### FR-HOLD-06 — Server Authority

Hold expiration shall be determined by server-side state and time.

The frontend timer shall only provide user feedback.

### FR-HOLD-07 — Held Seat Protection

A held seat shall not be successfully acquired by another customer while the hold is valid.

### FR-HOLD-08 — All-or-Nothing Multi-Seat Hold

A multi-seat hold shall succeed only when all requested seats can be acquired.

If any requested seat cannot be acquired, the system shall not partially hold the requested set.

---

# 13. Booking Requirements

### FR-BOOK-01 — Booking Creation

The system shall allow an authenticated customer to create a booking using a valid seat hold.

### FR-BOOK-02 — Booking Information

A booking shall contain sufficient information to represent:

* customer,
* show,
* selected seats,
* amount,
* booking status,
* creation information.

### FR-BOOK-03 — Booking State

The booking lifecycle shall support states equivalent to:

```text
INITIATED
HELD
PAYMENT_PENDING
CONFIRMED
TICKET_ISSUED
EXPIRED
CANCELLED
PAYMENT_FAILED
```

Exact implementation states may be refined during detailed design without violating the required business behavior.

### FR-BOOK-04 — Payment Requirement

A booking shall not become CONFIRMED before the required payment condition is satisfied.

### FR-BOOK-05 — Booking Retrieval

An authenticated customer shall be able to retrieve an individual booking.

### FR-BOOK-06 — Booking History

An authenticated customer shall be able to retrieve their booking history.

---

# 14. Payment Requirements

### FR-PAY-01 — Payment Initiation

The system shall allow payment to be initiated for a valid booking.

### FR-PAY-02 — Payment States

The payment workflow shall support:

```text
SUCCESS
FAILURE
CANCELLED
TIMEOUT
UNKNOWN
```

### FR-PAY-03 — Payment Processing Grace Period

If payment is initiated before the normal 5-minute hold expires, the booking may remain in PAYMENT_PENDING for a bounded grace period of 2 minutes.

### FR-PAY-04 — Failed Payment

A failed payment shall not result in a confirmed booking.

### FR-PAY-05 — Successful Payment

A successful payment shall allow booking confirmation if the booking remains valid.

### FR-PAY-06 — Payment Unknown

The system shall not blindly initiate another payment when a previous payment result is UNKNOWN.

### FR-PAY-07 — Payment Status Recovery

The system shall provide a mechanism to retrieve or reconcile uncertain payment status.

### FR-PAY-08 — Payment Idempotency

Repeated requests representing the same payment operation shall not result in duplicate payment effects.

### FR-PAY-09 — Payment Callback Idempotency

Repeated provider callbacks for the same payment operation shall not create duplicate booking/payment effects.

---

# 15. Booking Confirmation and Ticket Requirements

### FR-TICKET-01 — Confirmation

A booking shall be marked CONFIRMED only after the required payment condition has been satisfied.

### FR-TICKET-02 — Ticket Issuance

The system shall issue a ticket for a successfully confirmed booking.

### FR-TICKET-03 — Ticket Retrieval

The customer shall be able to retrieve the ticket associated with a confirmed booking.

### FR-TICKET-04 — Payment/Confirmation Failure

If payment succeeds but booking confirmation cannot be completed, the booking shall not incorrectly appear as confirmed.

The payment shall enter a refund/reversal recovery process.

---

# 16. Cancellation and Refund Requirements

### FR-CANCEL-01 — Policy Visibility

The applicable cancellation policy shall be available to the customer.

### FR-CANCEL-02 — Cancellation Eligibility

A booking shall only be cancellable when the associated show and cancellation policy permit cancellation.

### FR-CANCEL-03 — Cancellation Execution

The system shall update the booking state when a valid cancellation is performed.

### FR-CANCEL-04 — Refund Handling

A valid cancellation may result in a refund/reversal process depending on payment state and applicable policy.

### FR-CANCEL-05 — Refund Status

The system shall allow the status of a refund/reversal operation to be represented.

### FR-CANCEL-06 — No Guaranteed External Refund Time

The platform shall not promise an exact external refund completion time.

---

# 17. Theatre Operations Requirements

### FR-OP-01

A theatre operator shall be able to manage assigned theatres.

### FR-OP-02

A theatre operator shall be able to manage screens belonging to assigned theatres.

### FR-OP-03

A theatre operator shall be able to manage physical seats.

### FR-OP-04

A theatre operator shall be able to create and manage shows.

### FR-OP-05

A theatre operator shall be able to configure applicable pricing.

### FR-OP-06

A theatre operator shall be able to configure cancellation policy.

### FR-OP-07

A theatre operator shall be able to view relevant bookings.

### FR-OP-08

A theatre operator shall be able to view occupancy information.

---

# 18. Real-Time Availability Requirements

### FR-REALTIME-01

The system should provide a mechanism for connected clients to observe changes in seat state.

### FR-REALTIME-02

Seat-state updates should propagate within a few seconds under normal operating conditions.

No unrealistic exact latency guarantee is required at this stage.

### FR-REALTIME-03

Real-time updates shall not be treated as the correctness mechanism.

The server shall revalidate seat state during critical operations.

---

# 19. Idempotency Requirements

### FR-IDEM-01

The system shall prevent duplicate effects caused by retries of critical operations.

### FR-IDEM-02

Booking creation shall support idempotent processing.

### FR-IDEM-03

Payment initiation shall support idempotent processing.

### FR-IDEM-04

Payment callbacks shall support idempotent processing.

### FR-IDEM-05

A lost client response followed by a retry shall not create an additional booking or payment when the original operation already succeeded.

---

# 20. Failure and Recovery Requirements

### FR-FAIL-01 — Client Disconnect

A customer disconnecting from the frontend shall not cause a server-side hold to remain permanently active.

### FR-FAIL-02 — Hold Recovery

Expired or abandoned holds shall eventually release their seats.

### FR-FAIL-03 — Service Failure

Failure of a service shall not permanently lock seats.

### FR-FAIL-04 — Database Failure

The system shall preserve consistency of critical booking state across recoverable failures.

### FR-FAIL-05 — Payment Timeout

An unresolved payment operation shall eventually transition to a bounded timeout/recovery state.

### FR-FAIL-06 — Payment Unknown

UNKNOWN payment results shall enter a reconciliation/recovery path.

### FR-FAIL-07 — Confirmation Failure

If payment succeeds but booking confirmation fails, the system shall initiate the defined refund/reversal recovery process.

### FR-FAIL-08 — Duplicate Recovery

Recovery and retry operations shall remain idempotent.

---

# 21. High-Concurrency Requirements

### FR-CONC-01

The system shall support approximately 1,000 concurrent users/booking requests under defined load-test conditions.

### FR-CONC-02

The test shall include multiple concurrent users attempting to acquire the same or overlapping seats for the same show.

### FR-CONC-03

For any specific:

```text
(show_id, physical_seat_id)
```

the system shall permit no more than one successful active allocation.

### FR-CONC-04

The system shall produce zero double bookings under the defined concurrency test.

### FR-CONC-05

Competing requests shall receive a safe failure/conflict response when the requested seat can no longer be allocated.

### FR-CONC-06

Correctness shall not depend on a single application instance.

### FR-CONC-07

Correctness shall not depend on frontend state.

### FR-CONC-08

Correctness shall not depend on in-memory locking alone.

---

# 22. API Gateway Requirements

### FR-GATE-01

Client-facing API requests shall enter the backend through the API Gateway.

### FR-GATE-02

The Gateway shall route requests to the appropriate business service.

### FR-GATE-03

The Gateway shall use service discovery to locate service instances where applicable.

### FR-GATE-04

The Gateway shall provide a centralized entry point for the frontend.

---

# 23. Service Discovery Requirements

### FR-EUREKA-01

Business services shall register with Eureka.

### FR-EUREKA-02

Services requiring service-to-service communication shall discover target services through the service-discovery mechanism.

### FR-EUREKA-03

Service instances shall not depend on hardcoded instance addresses for normal service discovery.

---

# 24. Database Requirements

### FR-DB-01

Each business microservice shall own its persistent data.

### FR-DB-02

A business service shall not directly query another service's database.

### FR-DB-03

Cross-service references shall use identifiers rather than cross-service database foreign keys.

### FR-DB-04

Critical booking and seat-allocation state shall be persisted.

### FR-DB-05

The Booking Service shall own show-specific seat inventory and booking state.

---

# 25. Security Requirements

### FR-SEC-01

Protected APIs shall require valid JWT authentication.

### FR-SEC-02

The system shall validate JWT signature and expiration.

### FR-SEC-03

The system shall enforce role-based authorization.

### FR-SEC-04

The system shall enforce resource-level authorization where required.

### FR-SEC-05

User credentials and OTP-related information shall not be stored or logged insecurely.

### FR-SEC-06

Sensitive secrets shall be supplied through secure configuration rather than source code.

### FR-SEC-07

Incoming API data shall be validated.

### FR-SEC-08

The system shall use parameterized/persistence-framework mechanisms that prevent SQL injection.

### FR-SEC-09

Production deployment shall use HTTPS.

### FR-SEC-10

Payment callback handling shall validate that the callback is legitimate before changing payment state.

---

# 26. Non-Functional Requirements

## 26.1 Scalability

### NFR-SCAL-01

Business services shall be independently deployable.

### NFR-SCAL-02

Services shall be designed so that high-demand services can be scaled independently.

### NFR-SCAL-03

The Booking Service shall support the high-concurrency booking workload without relying on process-local locks.

---

# 27. Performance

### NFR-PERF-01

The system shall remain responsive under the defined normal workload.

### NFR-PERF-02

The system shall support the defined approximately 1,000-concurrent-user load test.

### NFR-PERF-03

Critical seat allocation shall prioritize correctness over optimistic response behavior.

### NFR-PERF-04

No arbitrary production latency target shall be assumed unless established through testing.

---

# 28. Reliability

### NFR-REL-01

Critical booking state shall be persistent.

### NFR-REL-02

Temporary failure shall not result in permanent seat locking.

### NFR-REL-03

The system shall provide recoverable states for uncertain payment operations.

### NFR-REL-04

Retrying a critical operation shall not create duplicate business effects.

---

# 29. Consistency

### NFR-CONS-01

Seat allocation shall maintain strong correctness for the same show and physical seat.

### NFR-CONS-02

The system shall enforce uniqueness of active allocation for:

```text
(show_id, physical_seat_id)
```

### NFR-CONS-03

Frontend seat state shall not override server-side authoritative state.

---

# 30. Maintainability

### NFR-MAINT-01

Each microservice shall have a clearly defined business responsibility.

### NFR-MAINT-02

Service boundaries shall be documented.

### NFR-MAINT-03

Each service shall follow a consistent internal application structure.

The planned backend structure is based on:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

### NFR-MAINT-04

Business logic shall not be unnecessarily duplicated across services.

---

# 31. Observability

### NFR-OBS-01

Services shall produce useful application logs.

### NFR-OBS-02

Logs shall include sufficient contextual information to trace important requests.

### NFR-OBS-03

Sensitive authentication and payment information shall not be written to logs.

### NFR-OBS-04

A correlation/request identifier should be propagated across service calls.

---

# 32. Usability

### NFR-USE-01

Customers shall receive clear seat states.

### NFR-USE-02

The interface shall clearly communicate the remaining hold/payment time.

### NFR-USE-03

Payment failures and timeouts shall be presented distinctly from successful bookings.

### NFR-USE-04

Cancellation eligibility and applicable policy shall be visible before cancellation.

### NFR-USE-05

When a requested seat becomes unavailable during a race, the user shall receive a clear failure message rather than an incorrect confirmation.

---

# 33. Deployment Requirements

### NFR-DEP-01

The application shall support online deployment.

### NFR-DEP-02

The academic implementation shall target zero-cost deployment.

### NFR-DEP-03

Environment-specific configuration shall not be hardcoded into application source code.

### NFR-DEP-04

Services shall be deployable independently where the deployment platform permits.

---

# 34. Availability and Failure Semantics

BookNGo does not require a claim of zero downtime.

Instead, the system shall prioritize:

* safe failure,
* consistency,
* recovery,
* bounded resource locking,
* and prevention of incorrect booking confirmation.

"Server down" shall therefore be interpreted as a failure/recovery scenario rather than a requirement for uninterrupted availability.

---

# 35. Booking State Model

The booking lifecycle shall support the following conceptual flow:

```text
INITIATED
    ↓
HELD
    ↓
PAYMENT_PENDING
    ↓
CONFIRMED
    ↓
TICKET_ISSUED
```

Alternative transitions include:

```text
HELD → EXPIRED
HELD → CANCELLED

PAYMENT_PENDING → PAYMENT_FAILED
PAYMENT_PENDING → EXPIRED

CONFIRMED → CANCELLED
CANCELLED → REFUND_PENDING
REFUND_PENDING → REFUNDED
```

The detailed implementation state machine shall preserve the business behavior defined here.

---

# 36. Seat State Model

The conceptual seat inventory lifecycle is:

```text
AVAILABLE
    ↓
HELD
    ↓
BOOKED
```

Expiration:

```text
HELD
  ↓
AVAILABLE
```

Cancellation of a confirmed booking may make the seat available again according to the booking/show lifecycle.

---

# 37. Critical Business Invariants

## INV-01 — No Double Booking

For the same show and physical seat:

```text
Successful active allocation ≤ 1
```

## INV-02 — One Active Hold

A physical seat shall not have multiple simultaneous active holds for the same show.

## INV-03 — Hold Expiration

An expired hold shall not continue blocking the seat indefinitely.

## INV-04 — Payment Confirmation

A failed payment shall not produce a confirmed booking.

## INV-05 — Retry Safety

Repeating a critical request shall not create duplicate business effects.

## INV-06 — Payment Recovery

An uncertain payment shall not be blindly charged again.

## INV-07 — Confirmation Integrity

A booking shall not appear confirmed unless the required confirmation conditions have been satisfied.

## INV-08 — Service Data Ownership

A service shall not directly modify another service's database.

---

# 38. Load-Test Requirement

The high-concurrency test shall simulate approximately 1,000 concurrent clients/requests.

The test should include:

```text
1. Same-show concurrent requests
2. Same-seat contention
3. Overlapping seat selections
4. Successful and rejected requests
5. Hold creation
6. Booking/payment simulation where appropriate
7. Retry scenarios
```

The test shall verify:

```text
Double bookings = 0
Invalid duplicate allocation = 0
System remains operational
```

The test does not require 1,000 real external payment transactions.

Payment may be simulated for load testing.

---

# 39. External Service Integration

## 39.1 Payment

The Payment Service shall act as the boundary between BookNGo and an external payment provider.

The initial implementation may use a simulated payment provider.

## 39.2 OTP

The User Service shall act as the boundary for OTP functionality.

The initial implementation may simulate OTP delivery.

External integrations must not own BookNGo's internal booking state.

---

# 40. Service Responsibility Traceability

| Requirement Area             | Primary Service |
| ---------------------------- | --------------- |
| Authentication               | User Service |
| Users                        | User Service |
| OTP                          | User Service |
| Movies                       | Movie           |
| Theatre                      | Theatre         |
| Screens                      | Theatre         |
| Physical Seats               | Theatre         |
| Theatre Operators            | Theatre         |
| Shows                        | Show            |
| Pricing                      | Show            |
| Cancellation Policy          | Show            |
| Show-specific Seat Inventory | Booking         |
| Seat Holds                   | Booking         |
| Booking                      | Booking         |
| Ticket                       | Booking         |
| Booking Cancellation         | Booking         |
| Payment                      | Payment         |
| Refund/Reversal              | Payment         |
| Payment Reconciliation       | Payment         |
| Routing                      | API Gateway     |
| Service Discovery            | Eureka          |

This mapping is a requirements traceability aid and will be validated during service decomposition and architecture design.

---

# 41. Acceptance-Level System Scenarios

## Scenario 1 — Successful Booking

```text
Customer authenticates
      ↓
Selects movie
      ↓
Selects theatre/show
      ↓
Views seats
      ↓
Selects seats
      ↓
Proceeds to booking
      ↓
Server creates hold
      ↓
Customer initiates payment
      ↓
Payment succeeds
      ↓
Booking confirmed
      ↓
Ticket issued
```

Expected result:

```text
Booking = CONFIRMED
Ticket = AVAILABLE
Seats = BOOKED
```

---

## Scenario 2 — Same Seat Concurrency

```text
Customer A ─┐
            ├──> Same Show + Same Seat
Customer B ─┘
```

Expected result:

```text
Successful allocation = 1
Double booking = 0
```

The losing request receives an appropriate conflict/unavailable response.

---

## Scenario 3 — Hold Expiry

```text
Customer creates hold
      ↓
5 minutes pass
      ↓
Hold expires
      ↓
Seat becomes AVAILABLE
```

Expected result:

```text
Seat can be acquired again
```

---

## Scenario 4 — Payment Failure

```text
Seat held
   ↓
Payment initiated
   ↓
Payment FAILURE
   ↓
Booking not confirmed
```

Expected result:

```text
Confirmed booking = NO
Seat eventually released according to state/hold rules
```

---

## Scenario 5 — Payment Unknown

```text
Payment initiated
      ↓
Provider result uncertain
      ↓
Payment = UNKNOWN
```

Expected result:

```text
No blind retry
Reconciliation/status path available
```

---

## Scenario 6 — Client Disconnect

```text
Customer obtains hold
      ↓
Customer disconnects
      ↓
No further action
      ↓
Server-side expiry
```

Expected result:

```text
Seat eventually becomes AVAILABLE
```

---

## Scenario 7 — Duplicate Request

```text
Request sent
   ↓
Operation succeeds
   ↓
Response lost
   ↓
Client retries
```

Expected result:

```text
Original business effect reused
No duplicate booking
No duplicate payment
```

---

## Scenario 8 — Payment Success but Confirmation Failure

```text
Payment SUCCESS
      ↓
Booking confirmation fails
      ↓
Recovery
      ↓
Refund / reversal
```

Expected result:

```text
Booking is not falsely shown as confirmed
Payment enters recoverable refund/reversal state
```

---

# 42. Requirements Traceability Principle

Every major implementation component shall be traceable to one or more requirements.

The development process should follow:

```text
PRD
 ↓
SRS
 ↓
Use Cases / User Stories
 ↓
Service Boundary
 ↓
Data Model
 ↓
API Contract
 ↓
Implementation
 ↓
Test
```

An implementation component should not be introduced solely because it is technically convenient if it creates unnecessary business complexity.

---

# 43. Requirements Change Control

The following decisions are considered important architectural/business constraints and should not be changed silently:

1. BookNGo is the product name.
2. The product is focused initially on cinema/entertainment ticket booking.
3. Physical seats belong to screens.
4. Seat availability is show-specific.
5. The concurrency identity is `(show_id, physical_seat_id)`.
6. Normal holds last 5 minutes.
7. Payment initiated during a valid hold receives a bounded 2-minute processing grace period.
8. The server is authoritative for hold expiration.
9. Failed payments cannot confirm bookings.
10. UNKNOWN payments require recovery/reconciliation.
11. Critical operations are idempotent.
12. Payment success followed by confirmation failure enters refund/reversal recovery.
13. Theatre operators manage only assigned theatres.
14. Cancellation is governed by show policy.
15. Booking owns show-specific seat inventory.
16. Services do not directly access each other's databases.
17. JWT is required for protected APIs.
18. Eureka is used for service discovery.
19. API Gateway is the centralized client-facing entry point.
20. The system must demonstrate concurrency correctness under the defined load test.

Any change to these decisions should be documented and reviewed against the PRD, SRS, architecture, ERD, API contracts, and implementation impact.

---

# 44. SRS Completion Status

This SRS translates the current BookNGo product definition into software-level requirements.

The next requirements artifacts should define:

```text
User Stories
      ↓
Actors and Use Cases
      ↓
Acceptance Criteria
```

These will then be used to validate the proposed microservice decomposition before the architecture and ERD are finalized.
