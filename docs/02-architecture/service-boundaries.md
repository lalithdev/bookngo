# BookNGo — Service Boundaries

## 1. Purpose

This document defines the responsibilities, ownership boundaries, and interaction boundaries of the six business microservices in BookNGo.

The purpose is to establish clear service ownership before designing the detailed architecture, database schemas, API contracts, and implementation.

The service decomposition baseline is:

1. User & Identity Service
2. Movie Service
3. Theatre Service
4. Show Service
5. Booking Service
6. Payment Service

Infrastructure components:

7. Eureka Server
8. API Gateway

Each business microservice owns its business data and the operations associated with that data.

No service should directly modify another service's database.

---

## 2. Boundary Principles

The BookNGo architecture follows these principles:

### 2.1 Single Business Ownership

Each important business concept has one authoritative service.

A concept should not be independently owned by multiple services.

### 2.2 Database Ownership

Each service owns its own persistent data.

Other services must access that information through APIs or defined service-to-service communication rather than directly accessing another service's database.

### 2.3 Business Responsibility Stays With the Owner

A service should execute business rules belonging to its domain.

For example:

- Movie Service manages movie information.
- Theatre Service manages physical cinema infrastructure.
- Show Service manages scheduled show information.
- Booking Service manages seat inventory for a show and booking lifecycle.
- Payment Service manages payment state and payment processing.
- User & Identity Service manages identity and authentication.

### 2.4 Strong Consistency Where Required

The Booking Service is the authoritative owner of show-specific seat allocation and must enforce the concurrency rules required to prevent double booking.

### 2.5 Avoid Artificial Fragmentation

A business capability should not become a separate microservice merely because it can technically be separated.

Seat allocation, seat holds, tickets, and cancellation execution remain within Booking Service because they are tightly coupled to the booking transaction and seat inventory lifecycle.

### 2.6 Infrastructure Is Not a Business Service

Eureka Server and API Gateway support the business services but do not own core BookNGo business data.

---

# 3. Service Overview

| Service | Primary Responsibility | Primary Data Ownership |
|---|---|---|
| User & Identity Service | User accounts, authentication, OTP, roles | Users, credentials, OTP records, roles |
| Movie Service | Movie catalogue and metadata | Movies, movie metadata |
| Theatre Service | Physical cinema infrastructure | Theatres, screens, physical seats, theatre operators |
| Show Service | Scheduled movie screenings and configuration | Shows, pricing, cancellation policy |
| Booking Service | Seat inventory, holds, bookings and tickets | Show-seat inventory, holds, bookings, tickets |
| Payment Service | Payment lifecycle and refund/reversal handling | Payment attempts, payment state, refunds/reversals |

---

# 4. User & Identity Service

## 4.1 Responsibility

The User & Identity Service manages the identity and authentication domain of BookNGo.

It provides functionality required to:

- Register users.
- Maintain user profiles.
- Authenticate users.
- Verify OTP-based authentication.
- Manage roles.
- Issue authentication credentials/tokens.
- Validate identity-related information required by other services.

## 4.2 Owns

The service owns:

- User account information.
- Authentication credentials.
- Phone number associated with an account.
- OTP records and OTP verification state.
- User roles.
- Authentication-related metadata.

Possible roles include:

- CUSTOMER
- THEATRE_OPERATOR
- ADMIN

## 4.3 Does Not Own

The service does not own:

- Movies.
- Theatres.
- Screens.
- Physical seats.
- Shows.
- Seat availability.
- Seat holds.
- Bookings.
- Payments.
- Tickets.

## 4.4 Key Rules

- Authentication must be required for protected booking operations.
- JWT is used for authenticated API access.
- OTP verification must follow the defined validity and rate-limiting rules.
- Role information is used for authorization.
- The service does not decide whether a seat is available.

---

# 5. Movie Service

## 5.1 Responsibility

The Movie Service manages the movie catalogue used by BookNGo.

It provides functionality to:

- Create movies.
- Update movie information.
- Remove or deactivate movies where permitted.
- Retrieve movie details.
- Search/filter movies based on supported metadata.

## 5.2 Owns

The service owns movie information such as:

- Movie ID.
- Title.
- Description.
- Language.
- Genre.
- Duration.
- Release information.
- Poster/image references.
- Format-related metadata where applicable.

## 5.3 Does Not Own

The service does not own:

- Theatres.
- Screens.
- Physical seats.
- Show schedules.
- Seat availability.
- Booking records.
- Payment records.
- User accounts.

## 5.4 Key Boundary

Movie Service answers:

> "What movie is this?"

It does not answer:

> "Where and when is this movie playing?"

Show Service owns that information.

---

# 6. Theatre Service

## 6.1 Responsibility

The Theatre Service manages the physical cinema infrastructure.

It represents the physical structure in which shows take place.

It provides functionality to:

- Create and manage theatres.
- Manage theatre information and location.
- Manage theatre operators and assignments.
- Create and manage screens.
- Define physical seats belonging to screens.
- Maintain physical seat configuration.

## 6.2 Owns

The service owns:

### Theatre

- Theatre ID.
- Theatre name.
- Location.
- Address.
- Operational information.

### Screen

- Screen ID.
- Theatre association.
- Screen name/number.
- Screen configuration.

### Physical Seat

- Physical seat ID.
- Screen association.
- Row.
- Seat number/label.
- Seat category where applicable.

### Theatre Operator

- Theatre operator assignment information.
- Theatre-to-operator relationship.

## 6.3 Does Not Own

The service does not own:

- Movies.
- Show schedules.
- Show-specific seat availability.
- Active seat holds.
- Bookings.
- Payments.
- Tickets.

## 6.4 Physical Seat Boundary

A physical seat belongs permanently to a screen.

The relationship is:

```text
Theatre
   |
   +-- Screen
         |
         +-- Physical Seat

A physical seat is infrastructure.

It is not the same thing as the availability of that seat for a particular show.

7. Show Service
7.1 Responsibility

The Show Service manages scheduled movie screenings and show-specific configuration.

It determines which movie is scheduled on which screen and at what time.

It provides functionality to:

Create shows.
Update shows.
Cancel shows where permitted.
Retrieve show details.
Configure show-specific pricing.
Configure cancellation policy.
Support show discovery/filtering.
7.2 Owns

The service owns:

Show

A show contains information such as:

Show ID.
Movie reference.
Theatre/screen reference.
Start time.
End time or derived duration.
Show status.
Format information where applicable.
Pricing

The service owns pricing configuration for a show.

Examples:

Regular.
Premium.
VIP.
Cancellation Policy

The service owns:

Whether the show is cancellable.
Cancellation deadline.
Relevant show-level cancellation configuration.
7.3 Does Not Own

The service does not own:

User accounts.
Physical seat definitions.
Active seat holds.
Real-time seat availability.
Booking records.
Payment records.
Ticket records.
7.4 Key Boundary

Show Service answers:

"What show is scheduled, where, when, and at what configured price?"

Booking Service answers:

"Which seats are currently available, held, or booked for that show?"

8. Booking Service
8.1 Responsibility

The Booking Service is the core transaction and seat-allocation service.

It manages the lifecycle from seat selection/hold through booking confirmation and ticket issuance.

It provides functionality to:

Create seat holds.
Release expired holds.
Maintain show-specific seat availability.
Create bookings.
Process booking state transitions.
Handle booking idempotency.
Confirm bookings after successful payment.
Issue tickets.
Execute permitted cancellations.
Maintain booking history.
Expose current seat state.
8.2 Owns

The service owns:

Show-Seat Inventory

The Booking Service maintains the show-specific state of physical seats.

Conceptually:

Show + Physical Seat = Show-Seat Inventory

A show-seat inventory record can have states such as:

AVAILABLE
HELD
BOOKED
Seat Hold

The service owns:

Hold ID.
Booking/session association.
Show.
Selected seats.
Hold creation time.
Hold expiration time.
Hold state.
Booking

The service owns:

Booking ID.
Customer reference.
Show reference.
Selected seat references.
Amount.
Booking state.
Timestamps.
Idempotency information.
Ticket

The service owns ticket information generated after successful booking confirmation.

8.3 Does Not Own

The Booking Service does not own:

User credentials.
Movie metadata.
Theatre physical seat definitions.
Show scheduling.
Payment provider credentials or payment transaction state.
8.4 Critical Concurrency Boundary

The Booking Service is the authoritative service for seat allocation.

The fundamental concurrency key is:

(show_id, physical_seat_id)

The system must guarantee that the same physical seat for the same show cannot be successfully allocated to more than one active booking.

For example:

Show A + Seat A10

is independent from:

Show B + Seat A10

because the same physical seat can be used for different shows at different times.

8.5 Hold Ownership

Seat holds belong to Booking Service.

A hold begins only after the customer confirms the selected seats and the server successfully creates the hold.

The normal hold duration is:

5 minutes

The server is authoritative for hold expiration.

The frontend timer is only a visual representation.

8.6 Payment Grace Boundary

If payment is initiated while a valid hold exists, the booking may enter:

PAYMENT_PENDING

A bounded payment-processing grace period of:

2 minutes

may be applied.

This prevents an unresolved payment operation from locking seats indefinitely.

8.7 Cancellation Boundary

The Show Service owns the cancellation policy configuration.

The Booking Service owns the execution of a cancellation against an actual booking.

Therefore:

Show Service
    |
    +-- defines cancellation policy

Booking Service
    |
    +-- applies cancellation to booking

Payment Service handles the associated financial refund/reversal operation.

9. Payment Service
9.1 Responsibility

The Payment Service manages the financial transaction lifecycle associated with bookings.

It provides functionality to:

Initiate payments.
Track payment attempts.
Process payment outcomes.
Handle idempotency.
Record payment state.
Handle payment timeout/unknown states.
Initiate refund/reversal workflows when required.
Support payment status/reconciliation.
9.2 Owns

The service owns:

Payment Attempt
Payment ID.
Booking reference.
Amount.
Payment status.
Provider/reference information where applicable.
Idempotency key.
Timestamps.
Payment State

Possible states include:

INITIATED
SUCCESS
FAILURE
CANCELLED
TIMEOUT
UNKNOWN
Refund/Reversal

The service owns refund/reversal records and their state.

9.3 Does Not Own

The service does not own:

Users.
Movies.
Theatres.
Screens.
Physical seats.
Seat availability.
Seat holds.
Booking lifecycle.
Ticket issuance.
9.4 Critical Payment Boundary

Payment success does not by itself mean that a booking is confirmed.

The Booking Service must confirm the booking only when the required booking and payment conditions are satisfied.

An exceptional flow may occur:

Payment SUCCESS
       |
       v
Booking confirmation failure
       |
       v
REFUND_PENDING
       |
       v
REFUNDED

The Payment Service manages the financial recovery process.

9.5 Idempotency

Payment operations must be idempotent.

A repeated request caused by:

client retry,
timeout,
lost response,
network failure,

must not result in duplicate payment processing.

Payment status/reconciliation is required for UNKNOWN outcomes so that the system does not blindly create another charge.

10. Cross-Service Ownership Matrix
Business Concept	Owning Service	Other Services May
User	User & Identity	Reference user
Authentication	User & Identity	Validate/use identity
Role	User & Identity	Enforce authorization
Movie	Movie	Reference movie
Theatre	Theatre	Reference theatre
Screen	Theatre	Reference screen
Physical Seat	Theatre	Reference seat
Show	Show	Reference show
Pricing	Show	Retrieve price
Cancellation Policy	Show	Read policy
Show-Seat Availability	Booking	Query availability
Seat Hold	Booking	Request/create/release through Booking
Booking	Booking	Reference booking
Ticket	Booking	Reference ticket
Payment	Payment	Initiate/query payment
Refund/Reversal	Payment	Request through Payment
Booking Cancellation	Booking	Trigger payment recovery through Payment
11. Key Data Relationships Across Services

The services use references rather than duplicated ownership.

Conceptually:

User & Identity
       |
       | user_id
       v
    Booking
       |
       | show_id
       v
     Show
      / \
     /   \
movie_id  screen_id
   |         |
   v         v
 Movie     Theatre
              |
              v
           Physical
             Seat

Payment is associated with the booking:

Booking
   |
   | booking_id
   v
Payment

The show-specific seat inventory is managed by Booking:

Show
  |
  +---- Physical Seat
             |
             v
      Show-Seat Inventory
             |
       +-----+-----+
       |     |     |
   AVAILABLE HELD BOOKED
12. Cross-Service Reference Rules
12.1 IDs Instead of Shared Tables

Services should store references such as:

user_id
movie_id
theatre_id
screen_id
show_id
physical_seat_id
booking_id
payment_id

rather than copying the complete entity owned by another service.

12.2 No Cross-Service Database Access

For example:

Booking Service
      X
      |
      X----> Movie Database

is not allowed.

Instead:

Booking Service
      |
      +----> Movie Service API

or an appropriate asynchronous/event-based mechanism may be used where justified.

12.3 Local Copies / Read Models

A service may maintain limited local information required for performance or resilience, but such information must not become a second authoritative source of truth.

The owning service remains authoritative.

13. Important Boundary Decisions
13.1 Physical Seat vs Show Seat

Physical seat:

Theatre Service

Show-specific availability:

Booking Service

This separation allows the same physical seat to participate independently in multiple shows.

13.2 Seat Availability vs Seat Definition

The Theatre Service knows that:

Screen 1 contains seat A10.

The Booking Service knows that:

A10 is AVAILABLE for Show 101.

These are different responsibilities.

13.3 Seat Hold vs Physical Seat

The Theatre Service defines the physical seat.

The Booking Service creates temporary holds against that seat for a particular show.

Therefore the Theatre Service does not manage:

hold timers,
booking locks,
seat availability,
booking concurrency.
13.4 Ticket vs Booking

The Booking Service owns both booking and ticket information because ticket issuance is a direct consequence of successful booking confirmation.

A separate Ticket Service is not introduced at this stage.

13.5 Refund vs Booking Cancellation

Booking Service determines whether a booking cancellation is allowed and changes the booking state.

Payment Service handles the financial refund/reversal operation.

13.6 Pricing vs Payment

Show Service owns the configured price for the show/seat category.

Payment Service processes the monetary transaction.

Therefore:

Show Service
    |
    +-- "How much should this booking cost?"

Payment Service
    |
    +-- "Was that amount successfully paid?"
14. Service Interaction Responsibilities

The following high-level interactions are expected.

Authentication
Client
  |
  v
API Gateway
  |
  v
User & Identity Service
Movie Discovery
Client
  |
  v
API Gateway
  |
  v
Movie Service
Theatre / Screen Information
Client
  |
  v
API Gateway
  |
  v
Theatre Service
Show Discovery
Client
  |
  v
API Gateway
  |
  v
Show Service

Show Service may obtain/reference movie and theatre information as required.

Seat Availability
Client
  |
  v
API Gateway
  |
  v
Booking Service

Booking Service is authoritative for current show-seat state.

Booking
Client
  |
  v
API Gateway
  |
  v
Booking Service
  |
  v
Payment Service

The exact communication pattern will be defined in the communication architecture document.

15. High-Level Booking Responsibility

The booking workflow crosses multiple service boundaries but each service retains its own responsibility.

Conceptually:

1. User authenticates
        |
        v
2. User discovers movie
        |
        v
3. User selects theatre/show
        |
        v
4. Booking Service provides seat availability
        |
        v
5. Booking Service creates hold
        |
        v
6. Payment Service processes payment
        |
        v
7. Booking Service confirms booking
        |
        v
8. Booking Service issues ticket

The workflow does not imply a distributed database transaction.

Each service remains responsible for its own state.

16. Failure Boundary Expectations

A service failure must not cause another service to permanently corrupt its own state.

Examples:

Payment Failure

Payment failure must not result in a confirmed booking.

Booking Failure

A booking failure must not permanently leave a seat locked.

User Service Failure

Authentication failure prevents protected operations but does not modify seat inventory.

Theatre Service Failure

Failure to retrieve theatre metadata must not directly alter existing booking state.

Payment Unknown

An unknown payment result must be recoverable through payment status/reconciliation rather than triggering an uncontrolled duplicate payment.

Detailed failure handling will be defined separately in:

docs/02-architecture/failure-handling.md
17. Scalability Boundaries

The decomposition allows individual services to scale independently.

The most concurrency-sensitive service is:

Booking Service

because it manages:

show-seat inventory,
seat holds,
concurrent booking attempts,
booking state transitions.

Other services have different workload characteristics:

Movie Service
    -> read-heavy catalogue access

Theatre Service
    -> relatively stable infrastructure data

Show Service
    -> show discovery and scheduling operations

User & Identity Service
    -> authentication and identity operations

Payment Service
    -> transaction-oriented workload

Booking Service
    -> high-concurrency seat allocation

The architecture should therefore avoid assuming that all services require identical scaling strategies.

18. Final Service Boundary

The final business boundary is:

+----------------------------+
| User & Identity Service    |
|----------------------------|
| Users                      |
| Authentication             |
| OTP                        |
| Roles                      |
+----------------------------+

+----------------------------+
| Movie Service              |
|----------------------------|
| Movies                     |
| Movie Metadata             |
+----------------------------+

+----------------------------+
| Theatre Service            |
|----------------------------|
| Theatres                   |
| Screens                    |
| Physical Seats             |
| Theatre Operators          |
+----------------------------+

+----------------------------+
| Show Service               |
|----------------------------|
| Shows                      |
| Pricing                    |
| Cancellation Policy        |
+----------------------------+

+----------------------------+
| Booking Service            |
|----------------------------|
| Show-Seat Inventory        |
| Seat Holds                 |
| Bookings                   |
| Tickets                    |
| Booking Cancellation       |
+----------------------------+

+----------------------------+
| Payment Service             |
|----------------------------|
| Payments                   |
| Payment State              |
| Refunds / Reversals        |
| Reconciliation             |
+----------------------------+

Infrastructure:

+----------------------------+
| Eureka Server              |
|----------------------------|
| Service Registration       |
| Service Discovery          |
+----------------------------+

+----------------------------+
| API Gateway                |
|----------------------------|
| External Entry Point       |
| Routing                    |
| Authentication Boundary    |
| Cross-Cutting Controls     |
+----------------------------+
19. Final Decision

BookNGo will use six business microservices:

User & Identity Service
Movie Service
Theatre Service
Show Service
Booking Service
Payment Service

with:

Eureka Server
API Gateway

as infrastructure components.

The most important ownership decision is:

Physical Seat
    -> Theatre Service

Show-Specific Seat Availability
    -> Booking Service

Seat Hold
    -> Booking Service

Booking
    -> Booking Service

Payment
    -> Payment Service

Refund/Reversal
    -> Payment Service

These boundaries are the baseline for the next architecture documents.

No database schema, API contract, or implementation should redefine these ownership boundaries without an explicit architecture decision.

