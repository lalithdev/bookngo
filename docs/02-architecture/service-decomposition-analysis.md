# BookNGo — Service Decomposition Analysis

## 1. Purpose

This document defines and justifies the microservice boundaries for the BookNGo platform.

The decomposition is derived from the BookNGo requirements and the PS003 problem statement.

The problem statement identifies the following as sample services:

- Movie Service
- Show Service
- Booking Service
- User Service

These services are treated as the baseline. The problem statement explicitly allows the sample microservices to be modified, extended, or replaced when the system requirements justify a different design.

The final decomposition is based on:

- Business cohesion
- Data ownership
- Consistency requirements
- Concurrency requirements
- Scalability
- Failure isolation
- Implementation complexity

The objective is to create meaningful independently deployable business services without unnecessarily fragmenting tightly coupled capabilities.

---

## 2. Decomposition Principles

### 2.1 Business Cohesion

Capabilities that belong to the same business domain and normally change together should remain within the same service.

### 2.2 Data Ownership

Each microservice owns the data belonging to its business domain.

A service must not directly modify another service's database.

Cross-service relationships are represented using identifiers and service APIs or appropriate asynchronous communication.

### 2.3 Consistency Boundary

Capabilities that require strong consistency should remain within the same transactional boundary when separating them would introduce unnecessary distributed transaction complexity.

This is especially important for seat allocation.

### 2.4 Concurrency Boundary

The booking workflow contains the most critical concurrency requirement in BookNGo.

The service responsible for show-specific seat inventory must also control the operation that determines whether a seat can be held or booked.

### 2.5 Independent Scalability

A capability should be separated when it has a meaningful requirement to scale independently.

Booking and payment have different scaling characteristics from movie administration, for example.

### 2.6 Failure Isolation

A capability should be separated when its failure characteristics are sufficiently different from another domain.

Payment processing is therefore separated from Booking because it has independent processing state, timeout, retry, idempotency, reconciliation, and external-provider failure scenarios.

### 2.7 Avoid Unnecessary Distribution

A capability should not become a separate microservice merely because it can be named independently.

Unnecessary decomposition can introduce:

- Additional network calls
- Additional service discovery
- More deployment units
- More failure points
- Distributed transaction complexity
- Increased development and testing effort

The goal is meaningful service boundaries rather than maximizing the number of services.

---

## 3. Candidate Capabilities

The following BookNGo capabilities were evaluated during decomposition:

1. Movie
2. Location
3. Theatre
4. Screen
5. Seat
6. Show
7. Pricing
8. Seat Availability
9. Seat Hold
10. Booking
11. Payment
12. Authentication
13. Ticket
14. Cancellation
15. Refund
16. Occupancy

---

## 4. Capability Analysis

| Capability | Proposed Service | Decomposition Decision | Reason |
|---|---|---|---|
| Movie | Movie Service | Separate | Movie metadata and discovery form a clear catalog domain and directly correspond to the problem statement's Movie Service. |
| Location | Theatre Service | Merge | Location is primarily used to identify and filter theatres and does not require an independent transactional boundary. |
| Theatre | Theatre Service | Separate | Theatre is the primary physical venue domain and owns theatre configuration. |
| Screen | Theatre Service | Merge | A screen is a physical component of a theatre and does not require independent ownership. |
| Seat | Theatre Service | Merge | Physical seats permanently belong to screens and therefore belong to the physical theatre domain. |
| Show | Show Service | Separate | Show scheduling has an independent business lifecycle and directly corresponds to the problem statement's Show Service. |
| Pricing | Show Service | Merge | Pricing for the current scope is associated with scheduled shows and does not require an independent service. |
| Seat Availability | Booking Service | Merge | Availability is specific to a show and physical seat and must be controlled within the booking concurrency boundary. |
| Seat Hold | Booking Service | Merge | A hold is a temporary state within the booking lifecycle and must be controlled together with seat inventory. |
| Booking | Booking Service | Separate | Booking is the central reservation domain and directly corresponds to the problem statement's Booking Service. |
| Payment | Payment Service | Separate | Payment has independent state, retry, timeout, idempotency, reconciliation, and external-provider interaction. |
| Authentication | User Service | Separate | Authentication and identity form a security boundary shared by multiple business domains. |
| Ticket | Booking Service | Merge | A ticket is produced from a confirmed booking and does not require an independent business boundary for the current scope. |
| Cancellation | Booking Service | Merge | Cancellation changes the booking lifecycle. The cancellation policy itself is configured with the Show domain. |
| Refund | Payment Service | Merge | Refund is a financial operation and belongs to the payment domain. |
| Occupancy | Derived capability | Merge | Occupancy is derived from show capacity and booking data and does not require an independent transactional service. |

---

## 5. Final Business Microservices

The decomposition results in six business microservices.

### 5.1 User Service

Responsible for identity and access-related functionality.

Responsibilities include:

- User registration
- User profile
- Authentication
- OTP verification
- JWT-related operations
- Roles
- Theatre operator accounts
- Theatre operator assignment

Primary data ownership:

- Users
- Roles
- Authentication-related data
- OTP metadata
- Operator assignments

This service represents and extends the User Service identified in the problem statement.

---

### 5.2 Movie Service

Responsible for the movie catalog.

Responsibilities include:

- Movie creation and management
- Movie metadata
- Movie discovery
- Movie search and filtering
- Movie status

Primary data ownership:

- Movies
- Movie metadata

This service directly corresponds to the Movie Service identified in the problem statement.

---

### 5.3 Theatre Service

Responsible for the physical cinema infrastructure.

Responsibilities include:

- Theatre management
- Theatre location
- Screen management
- Physical seat configuration
- Seat categories
- Theatre operator access to assigned theatres

Primary data ownership:

- Theatres
- Theatre locations
- Screens
- Physical seats
- Seat categories

The physical hierarchy is:

```text
Theatre
    |
    +-- Screen
          |
          +-- Physical Seat
````

A physical seat belongs permanently to a screen.

The Theatre Service owns the definition of a physical seat, but it does not own whether that seat is currently available for a particular show.

---

### 5.4 Show Service

Responsible for scheduled movie screenings.

Responsibilities include:

* Show creation
* Show scheduling
* Show updates
* Show cancellation/deactivation
* Movie reference
* Theatre/screen reference
* Show date and time
* Language
* Format
* Pricing configuration
* Cancellation policy

Primary data ownership:

* Shows
* Show schedules
* Show pricing
* Cancellation policies

The Show Service represents the Show Service identified in the problem statement while incorporating the additional requirements for pricing and cancellation policy.

A show references:

```text
movieId
theatreId
screenId
```

The Show Service does not own the corresponding Movie, Theatre, or physical Seat records.

---

### 5.5 Booking Service

Responsible for the reservation and seat-allocation lifecycle.

Responsibilities include:

* Show-specific seat inventory
* Seat availability
* Seat holds
* Hold expiry
* Concurrency control
* Booking creation
* Booking state
* Booking idempotency
* Booking cancellation
* Ticket generation
* Booking history

Primary data ownership:

* Show-seat inventory
* Seat hold records
* Bookings
* Booking items
* Booking state
* Ticket information

This service represents the Booking Service identified in the problem statement.

It is also the critical consistency and concurrency boundary of BookNGo.

---

### 5.6 Payment Service

Responsible for payment processing and financial transaction state.

Responsibilities include:

* Payment initiation
* Payment attempts
* Payment status
* Payment idempotency
* Provider references
* Payment callbacks
* Payment timeout
* Unknown payment state
* Reconciliation
* Refund processing/state

Primary data ownership:

* Payments
* Payment attempts
* Provider transaction references
* Refund records

Payment is separated from Booking because payment processing can fail independently and may involve an external payment provider.

---

## 6. Important Capability Boundaries

### 6.1 Physical Seat vs Show-Seat Inventory

The system distinguishes between a physical seat and its availability for a show.

The Theatre Service owns the physical seat:

```text
Theatre
    |
    +-- Screen
          |
          +-- A10
```

The Booking Service owns the show-specific inventory:

```text
Show 101 + A10 → AVAILABLE
Show 102 + A10 → BOOKED
Show 103 + A10 → HELD
```

Therefore:

```text
Physical Seat ≠ Show-Seat Availability
```

The critical inventory identity is:

```text
(showId, physicalSeatId)
```

This ensures that the same physical seat can be used independently for different shows.

---

### 6.2 Seat Availability and Seat Hold

Seat availability and seat hold remain inside the Booking Service.

The booking workflow requires these operations to be closely coordinated:

```text
Check availability
       |
       v
Acquire seat
       |
       v
Create hold
       |
       v
Validate hold
       |
       v
Confirm booking
```

Separating Seat Availability or Seat Hold into independent services would distribute the critical concurrency boundary unnecessarily.

---

### 6.3 Cancellation

Cancellation has two different aspects.

#### Cancellation Policy

The Show Service owns the configuration of whether a show is cancellable and the applicable cancellation deadline.

Example:

```text
cancellable = true
deadline = 24 hours before show
```

#### Cancellation Execution

The Booking Service performs cancellation of an existing booking because cancellation changes the booking lifecycle and booking state.

Therefore:

```text
Show Service
    |
    +-- Cancellation Policy

Booking Service
    |
    +-- Booking Cancellation
```

A separate Cancellation Service is not required for the current scope.

---

### 6.4 Ticket

Ticket generation remains part of the Booking Service.

The lifecycle is:

```text
Booking
   |
   v
Payment Success
   |
   v
Booking Confirmed
   |
   v
Ticket Issued
```

The ticket is a result of a confirmed booking rather than an independent business transaction.

A separate Ticket Service may be introduced in a future version if ticketing becomes a sufficiently independent domain.

---

### 6.5 Refund

Refund remains within the Payment Service.

The responsibility is:

```text
Payment Service
    |
    +-- Payment
    |
    +-- Refund
```

Refund is directly associated with payment state and provider interaction.

The Booking Service may record the booking-level outcome, while the Payment Service owns the financial transaction state.

---

### 6.6 Pricing

Pricing remains within the Show Service for the current scope.

The current system uses relatively simple show-level seat-category pricing such as:

```text
Regular
Premium
VIP
```

A separate Pricing Service would add complexity without a strong independent business or scaling requirement.

---

### 6.7 Location

Location remains within the Theatre Service.

The system primarily uses location for:

* Theatre identification
* Theatre filtering
* Show discovery by location

The current requirements do not justify a standalone Location Service.

---

### 6.8 Occupancy

Occupancy is treated as a derived capability.

For example:

```text
Screen Capacity = 100
Confirmed Seats = 75

Occupancy = 75%
```

Occupancy can be calculated from show capacity and booking information.

A separate Occupancy Service is therefore not required for the current system.

---

## 7. Booking Concurrency Boundary

The Booking Service owns the critical show-seat allocation operation.

For a specific:

```text
showId + physicalSeatId
```

the system must guarantee:

```text
Successful allocation <= 1
```

when multiple users attempt to acquire the same seat concurrently.

Conceptually:

```text
                 Booking Service
                       |
              Show-Seat Inventory
                       |
              Concurrency Control
                       |
             +---------+---------+
             |                   |
          User A              User B
             |                   |
             +---------+---------+
                       |
                  One succeeds
```

The correctness of seat allocation must remain independent of the number of Booking Service instances.

---

## 8. Service Data Ownership

| Service                 | Primary Data Owned                                                    |
| ----------------------- | --------------------------------------------------------------------- |
| User Service | Users, roles, authentication data, OTP metadata, operator assignments |
| Movie Service           | Movies and movie metadata                                             |
| Theatre Service         | Theatres, locations, screens, physical seats, seat categories         |
| Show Service            | Shows, schedules, pricing, cancellation policies                      |
| Booking Service         | Show-seat inventory, holds, bookings, booking state, tickets          |
| Payment Service         | Payments, payment attempts, provider references, refunds              |

No service directly accesses another service's database.

Cross-service references use identifiers and service communication.

---

## 9. Problem Statement Alignment

The final decomposition retains all four sample services from the PS003 problem statement:

```text
Problem Statement              BookNGo

Movie Service             →    Movie Service

Show Service              →    Show Service

Booking Service           →    Booking Service

User Service              →    User Service
```

Additional services are introduced only where the requirements provide a clear boundary:

```text
Theatre Service
    → Physical cinema infrastructure

Payment Service
    → Payment and financial transaction boundary
```

This results in six business microservices.

The sample services from the problem statement therefore remain represented while the architecture is refined according to the actual BookNGo requirements.

---

## 10. Why Not Nine Microservices?

The following capabilities were intentionally not separated into individual microservices:

```text
Location
Screen
Seat
Pricing
Seat Availability
Seat Hold
Ticket
Cancellation
Refund
Occupancy
```

This is because they either:

* belong strongly to an existing business domain,
* require the same consistency boundary,
* are lifecycle stages of another domain,
* or are derived information.

For example:

```text
Theatre
    |
    +-- Screen
    |
    +-- Physical Seat
```

forms one physical infrastructure domain.

Similarly:

```text
Seat Availability
       |
       v
Seat Hold
       |
       v
Booking
       |
       v
Cancellation
       |
       v
Ticket
```

forms the reservation lifecycle.

And:

```text
Payment
    |
    +-- Refund
```

forms the financial transaction domain.

Separating each of these capabilities would create additional distributed communication and operational complexity without a corresponding requirement for independent business ownership.

---

## 11. Final Decomposition

The BookNGo business layer consists of:

```text
1. User Service
2. Movie Service
3. Theatre Service
4. Show Service
5. Booking Service
6. Payment Service
```

Infrastructure components are:

```text
7. Eureka Server
8. API Gateway
```

Eureka Server and API Gateway are infrastructure components and are not counted as business microservices.

---

## 12. Final Decision

The BookNGo architecture will use six business microservices.

The decomposition preserves the four sample services specified in the PS003 problem statement while introducing Theatre Service and Payment Service based on actual system requirements.

The most important boundary is the Booking Service, which owns show-specific seat inventory, holds, booking state, and concurrency control.

Payment is separated because it has an independent financial transaction lifecycle and failure boundary.

Theatre and Show are separated because physical cinema infrastructure and scheduled screenings represent distinct business responsibilities.

The remaining capabilities are retained within the service that naturally owns their business lifecycle rather than being split into artificial microservices.

This decomposition is the baseline for the next architecture phase.

---
