# BookNGo — Architecture Overview

## 1. Purpose

This document defines the high-level architecture of the BookNGo platform.

It translates the approved service boundaries into a deployable microservices architecture and establishes:

- System components.
- Infrastructure components.
- Service relationships.
- Request flow.
- High-level data flow.
- External system boundaries.
- Architectural responsibilities.
- Scalability considerations.
- Major booking workflow.

Detailed communication patterns, data ownership, concurrency mechanisms, failure handling, security implementation, and API contracts are defined in separate architecture documents.

---

# 2. Architectural Style

BookNGo follows a **microservices architecture**.

The system consists of independently deployable business services supported by infrastructure components for:

- Service discovery.
- Centralized request routing.
- Authentication/security enforcement.
- Independent service scaling.

The architecture is organized around business capabilities rather than technical layers alone.

The six business microservices are:

1. User Service
2. Movie Service
3. Theatre Service
4. Show Service
5. Booking Service
6. Payment Service

Infrastructure components:

7. Eureka Server
8. API Gateway

The frontend is a separate client application.

---

# 3. High-Level System Architecture

```text
                              +----------------------+
                              |      End User        |
                              |   Web Browser        |
                              +----------+-----------+
                                         |
                                         | HTTPS
                                         v
                              +----------------------+
                              |     React Frontend   |
                              |       Vite           |
                              +----------+-----------+
                                         |
                                         | REST / HTTPS
                                         v
                              +----------------------+
                              |      API Gateway     |
                              |   Spring Cloud       |
                              |      Gateway         |
                              +----------+-----------+
                                         |
                           Service Discovery / Routing
                                         |
                  +------------------+---+---+------------------+
                  |                  |       |                  |
                  v                  v       v                  v
        +----------------+  +----------------+  +----------------+
        | User Service|  | Movie Service  |  | Theatre        |
        |    Service     |  |                |  | Service        |
        +-------+--------+  +-------+--------+  +-------+--------+
                |                   |                   |
                v                   v                   v
        +---------------+   +---------------+   +---------------+
        | User Database |   | Movie Database |   | Theatre DB    |
        +---------------+   +---------------+   +---------------+

                  +------------------+------------------+
                  |                                     |
                  v                                     v
        +----------------+                     +----------------+
        |  Show Service  |                     | Booking        |
        |                |                     | Service        |
        +-------+--------+                     +-------+--------+
                |                                      |
                v                                      v
        +---------------+                      +---------------+
        |  Show Database|                      | Booking DB    |
        +---------------+                      +---------------+
                                                       |
                                                       |
                                                       v
                                              +----------------+
                                              | Payment        |
                                              | Service        |
                                              +-------+--------+
                                                      |
                                                      v
                                              +----------------+
                                              | Payment DB     |
                                              +----------------+

                         +----------------------+
                         |    Eureka Server     |
                         |  Service Discovery   |
                         +----------------------+

The diagram represents logical architecture.

Physical deployment topology may differ depending on the deployment environment.

4. Architectural Components
4.1 React Frontend

The frontend is responsible for the user-facing application.

Technology:

Vite
React
JavaScript
HTML
CSS
Tailwind CSS

Responsibilities include:

Movie discovery UI.
Theatre and show discovery UI.
Seat selection UI.
Seat availability display.
Hold countdown display.
Authentication UI.
Booking UI.
Payment UI.
Ticket display.
Booking history.
Cancellation interface.
Theatre operator interfaces where authorized.

The frontend does not own authoritative business state.

In particular:

The frontend timer does not determine hold expiry.
The frontend does not decide whether a seat is available.
The frontend does not confirm payment.
The frontend does not create a booking independently.

The backend remains authoritative.

5. API Gateway

The API Gateway is the external entry point for backend APIs.

It provides centralized handling for:

Request routing.
Service discovery integration.
Authentication-related gateway processing where appropriate.
Cross-cutting request controls.
CORS configuration.
Centralized API entry.
Request/response handling.

Clients should normally communicate with backend services through the API Gateway rather than directly addressing individual services.

Conceptually:

Frontend
    |
    v
API Gateway
    |
    +----> User Service
    |
    +----> Movie Service
    |
    +----> Theatre Service
    |
    +----> Show Service
    |
    +----> Booking Service
    |
    +----> Payment Service

The Gateway does not own business data.

6. Eureka Server

Eureka Server provides service discovery.

Each backend service registers itself with Eureka.

Conceptually:

User Service ----+
Movie Service --------------+
Theatre Service ------------+
Show Service ---------------+----> Eureka Server
Booking Service ------------+
Payment Service ------------+

The API Gateway and backend services can use service discovery to locate registered services.

Eureka does not process business transactions.

It owns service registration information rather than BookNGo business entities.

7. Business Services
7.1 User Service

Responsible for:

User registration.
Authentication.
OTP verification.
User profile information.
Roles.
JWT-related identity operations.

Database:

User Database

The service is the authoritative owner of user identity information.

7.2 Movie Service

Responsible for:

Movie catalogue.
Movie metadata.
Movie search and discovery.

Database:

Movie Database

The service is the authoritative owner of movie information.

7.3 Theatre Service

Responsible for:

Theatre information.
Theatre locations.
Screens.
Physical seats.
Theatre operator assignments.

Database:

Theatre Database

The service is the authoritative owner of physical cinema infrastructure.

7.4 Show Service

Responsible for:

Show schedules.
Movie/show association.
Theatre/screen/show association.
Show status.
Pricing.
Cancellation policy.

Database:

Show Database

The service is the authoritative owner of scheduled shows and show configuration.

7.5 Booking Service

Responsible for:

Show-specific seat inventory.
Seat availability.
Seat holds.
Booking lifecycle.
Booking idempotency.
Ticket issuance.
Booking cancellation execution.

Database:

Booking Database

The Booking Service is the authoritative owner of the live show-seat allocation state.

This is the most concurrency-sensitive business service.

7.6 Payment Service

Responsible for:

Payment initiation.
Payment attempts.
Payment state.
Payment idempotency.
Payment timeout/unknown handling.
Refund/reversal workflows.
Payment reconciliation.

Database:

Payment Database

The Payment Service is the authoritative owner of payment state.

8. Database-per-Service Principle

Each business service owns its own database.

The logical arrangement is:

User Service
        |
        v
   User Database

Movie Service
        |
        v
   Movie Database

Theatre Service
        |
        v
  Theatre Database

Show Service
        |
        v
   Show Database

Booking Service
        |
        v
  Booking Database

Payment Service
        |
        v
  Payment Database

A service must not directly read or modify another service's database.

For example:

Booking Service
      X
      |
      X----> Show Database

Instead, the Booking Service communicates with Show Service through an approved service interface.

This preserves service autonomy and ownership.

9. Service Relationships

The primary conceptual relationships are:

User Service
        |
        | user identity
        v
    Booking Service

Movie Service
        |
        | movie reference
        v
    Show Service

Theatre Service
        |
        | theatre/screen reference
        v
    Show Service

Show Service
        |
        | show reference
        v
    Booking Service

Theatre Service
        |
        | physical seat reference
        v
    Booking Service

Booking Service
        |
        | booking reference
        v
    Payment Service

These relationships represent references and interactions, not shared database ownership.

10. Core Domain Relationship

The core domain model spans multiple services.

Conceptually:

Movie
   |
   | scheduled as
   v
Show
   |
   | occurs on
   v
Screen
   |
   | belongs to
   v
Theatre
   |
   | contains
   v
Physical Seats

The live booking state is maintained separately:

Show
  +
Physical Seat
  |
  v
Show-Seat Inventory
  |
  +---- AVAILABLE
  |
  +---- HELD
  |
  +---- BOOKED

The Booking Service owns this show-specific inventory state.

11. Main User Request Flow

A typical request follows:

User
  |
  v
React Frontend
  |
  | HTTPS
  v
API Gateway
  |
  | Service Discovery
  v
Target Microservice
  |
  v
Service Database

If a request requires information from another service:

Target Microservice
        |
        v
Another Microservice
        |
        v
Its Own Database

The detailed communication mechanism will be defined in:

docs/02-architecture/communication.md
12. Movie Discovery Flow

A basic movie discovery request is:

User
  |
  v
Frontend
  |
  v
API Gateway
  |
  v
Movie Service
  |
  v
Movie Database
  |
  v
Movie Service
  |
  v
API Gateway
  |
  v
Frontend

Movie Service remains responsible for movie information.

13. Show Discovery Flow

Show discovery may involve information from multiple domains.

Conceptually:

User
  |
  v
Frontend
  |
  v
API Gateway
  |
  v
Show Service
  |
  +----> Show Database
  |
  +----> Required Theatre information
  |
  +----> Required Movie information
  |
  v
Response

The exact communication strategy and whether some read information is obtained through APIs, cached data, or another mechanism will be defined later.

No service is permitted to directly query another service's database.

14. Seat Availability Flow

Seat availability is handled by Booking Service.

User
  |
  v
Frontend
  |
  v
API Gateway
  |
  v
Booking Service
  |
  v
Booking Database
  |
  v
Show-Seat Inventory
  |
  v
AVAILABLE / HELD / BOOKED

The Booking Service may use references to Show, Screen, and Physical Seat entities owned by other services.

The current allocation state remains owned by Booking Service.

15. Seat Hold Flow

The hold begins only after the user confirms the selected seats.

Conceptually:

User
  |
  | Select seats
  v
Frontend
  |
  | Continue / Proceed to Book
  v
API Gateway
  |
  v
Booking Service
  |
  | Validate requested inventory
  |
  | Atomically acquire seats
  |
  v
Booking Database
  |
  v
HELD

The normal hold duration is:

5 minutes

The server determines the authoritative expiration time.

The frontend displays a countdown but does not control the actual hold.

16. High-Concurrency Booking Flow

The central concurrency-sensitive operation is seat allocation.

Suppose multiple users attempt to acquire:

Show 101 + Seat A10

at approximately the same time.

The requests may be:

User A ----+
User B ----+
User C ----+----> API Gateway ----> Booking Service
User D ----+
User E ----+

Booking Service must ensure that only one request successfully acquires the seat.

Conceptually:

                    +-------------------+
                    | Booking Service   |
                    +---------+---------+
                              |
                    Atomic inventory operation
                              |
                              v
                    +-------------------+
                    | Show 101 / A10    |
                    +-------------------+
                              |
                    +---------+---------+
                    |                   |
                    v                   v
                 Success              Reject
                 HELD/BOOKED          UNAVAILABLE

The detailed concurrency strategy will be defined separately in:

docs/02-architecture/concurrency-strategy.md
17. Booking and Payment Flow

The major transaction flow is:

Customer
   |
   v
Frontend
   |
   v
API Gateway
   |
   v
Booking Service
   |
   | Create hold
   v
Seat HELD
   |
   | Payment initiated
   v
Payment Service
   |
   | Payment result
   v
Booking Service
   |
   +---- SUCCESS ----> CONFIRMED ----> TICKET ISSUED
   |
   +---- FAILURE ---> Booking not confirmed
   |
   +---- TIMEOUT ---> Booking/payment recovery
   |
   +---- UNKNOWN --> Reconciliation

Payment processing does not directly own booking confirmation.

Booking Service remains responsible for the booking lifecycle.

18. Payment Grace Period

If payment is initiated while the normal seat hold is still valid, the booking may enter:

PAYMENT_PENDING

A bounded payment-processing grace period of:

2 minutes

may apply.

The purpose is to avoid permanently locking seats because of unresolved payment operations.

Conceptually:

5-minute normal hold
        |
        | payment initiated before expiry
        v
PAYMENT_PENDING
        |
        | maximum bounded grace
        v
Payment result

An unresolved payment must eventually transition into a recoverable timeout/unknown path.

19. Failure and Recovery Architecture

BookNGo must prioritize safe failure.

The architecture should prevent failures from producing permanent inconsistent business state.

Examples:

Booking Service Failure

Should not permanently leave seats locked.

Payment Service Failure

Should not automatically create a confirmed booking without a valid payment condition.

Client Network Failure

Should not cause a duplicate booking or duplicate payment when the client retries.

Payment UNKNOWN

Should not trigger an uncontrolled second payment attempt.

Service Restart

Persistent state must allow the service to recover its business state.

Detailed failure handling will be defined in:

docs/02-architecture/failure-handling.md
20. Authentication and Security Boundary

Protected API requests are authenticated using JWT.

Conceptually:

Client
  |
  | JWT
  v
API Gateway
  |
  v
Protected Microservice

The exact responsibilities of:

JWT issuance.
JWT validation.
Gateway authentication.
Service-level authorization.
Role-based access.

will be defined in:

docs/02-architecture/security.md

The User Service remains responsible for identity and authentication-related operations.

21. Theatre Operator Flow

Theatre operators have restricted access to their assigned theatres.

Conceptually:

Theatre Operator
       |
       v
Frontend
       |
       v
API Gateway
       |
       v
JWT / Role Validation
       |
       v
Authorized Service

Theatre operators may manage resources belonging to their assigned theatres, subject to authorization rules.

Expected management capabilities include:

Theatre information.
Screens.
Physical seats.
Shows.
Pricing.
Cancellation configuration.
Booking/occupancy visibility.

The detailed authorization model will be defined separately.

22. Admin Flow

Platform administrators have platform-level management responsibilities.

Initial scope includes:

Movie management.
Theatre operator management/assignment.
Basic platform oversight.

Admin functionality should remain limited to the required project scope rather than introducing unnecessary platform complexity.

23. External System Boundary

The architecture may integrate with external systems for:

Payment processing.
OTP/SMS delivery.

For Review 1, these integrations may be simulated where required to keep the system cost-free and independently testable.

The architecture must preserve the boundary between BookNGo and external providers.

Conceptually:

Payment Service
      |
      v
External Payment Provider

and:

User Service
      |
      v
OTP/SMS Provider

The external provider is not part of the BookNGo business microservice architecture.

24. Deployment Model

Each business microservice is independently deployable.

Logical deployment units:

Frontend
API Gateway
Eureka Server

User Service
Movie Service
Theatre Service
Show Service
Booking Service
Payment Service

Each service may run as an independent application/container.

Docker may be used to package the services.

A local development environment can use Docker Compose to run the complete architecture.

Deployment infrastructure may vary depending on the selected free hosting platform.

The logical service boundaries remain independent even if multiple services are temporarily deployed on the same physical host.

25. Scalability Model

The architecture allows services to scale independently.

The most critical service for high-concurrency scaling is:

Booking Service

because it handles:

Seat availability.
Seat holds.
Concurrent seat acquisition.
Booking state transitions.

Other services may experience different traffic characteristics.

For example:

Movie Service
    -> catalogue reads

Show Service
    -> show discovery and scheduling

Theatre Service
    -> relatively stable infrastructure data

User Service
    -> authentication traffic

Payment Service
    -> transaction-oriented workload

Booking Service
    -> high-concurrency inventory operations

The architecture therefore avoids requiring every service to scale identically.

26. Availability and Correctness Priorities

For the booking domain, correctness has priority over accepting every concurrent request.

The system must prefer:

Reject conflicting request

over:

Allow double allocation

For example:

100 concurrent attempts
        |
        v
One successful allocation
        +
Remaining requests receive
unavailable/conflict response

The exact load-testing methodology will be documented separately.

27. Architectural Constraints

The following constraints are established:

Constraint 1

There are six business microservices.

Constraint 2

Each business service owns its own data.

Constraint 3

No direct cross-service database access.

Constraint 4

API Gateway is the primary external backend entry point.

Constraint 5

Eureka provides service discovery.

Constraint 6

Booking Service owns show-specific seat availability and holds.

Constraint 7

Theatre Service owns physical seat definitions.

Constraint 8

Show Service owns show configuration, pricing, and cancellation policy.

Constraint 9

Payment Service owns payment and refund/reversal state.

Constraint 10

JWT is used for protected authenticated operations.

Constraint 11

Server-side state is authoritative over frontend state.

Constraint 12

Seat allocation must remain safe under concurrent requests.

28. Architectural Decision Summary
Decision	Selected Approach
Architecture style	Microservices
Business services	6
API entry point	API Gateway
Service discovery	Eureka
Authentication	JWT
Frontend	Vite + React
Backend	Spring Boot
Database	PostgreSQL
ORM	Hibernate/JPA
Database ownership	Database per service
Physical seat ownership	Theatre Service
Show-seat availability	Booking Service
Seat holds	Booking Service
Booking ownership	Booking Service
Payment ownership	Payment Service
Refund/reversal ownership	Payment Service
Deployment unit	Independently deployable service
Local orchestration	Docker Compose
High-concurrency focus	Booking Service
Review payment	Simulation permitted
Review OTP	Simulation permitted
29. Architecture Baseline

The approved high-level architecture is:

                         +----------------+
                         |    Frontend    |
                         | React + Vite   |
                         +-------+--------+
                                 |
                                 v
                         +----------------+
                         |  API Gateway   |
                         +-------+--------+
                                 |
                         +-------+--------+
                         |    Eureka      |
                         | Service        |
                         | Discovery      |
                         +-------+--------+
                                 |
        +------------+------------+------------+------------+
        |            |            |            |            |
        v            v            v            v            v
+-------------+ +----------+ +----------+ +----------+ +----------+
| User &      | |  Movie   | | Theatre  | |   Show   | | Booking  |
| Identity    | | Service  | | Service  | | Service  | | Service  |
+------+------+ +----+-----+ +----+-----+ +----+-----+ +----+-----+
       |             |            |            |            |
       v             v            v            v            v
    User DB       Movie DB    Theatre DB     Show DB    Booking DB
                                                           |
                                                           v
                                                    +----------+
                                                    | Payment  |
                                                    | Service  |
                                                    +----+-----+
                                                         |
                                                         v
                                                    Payment DB

This architecture is the baseline for the remaining architecture design documents.

30. Next Architecture Documents

The architecture should now be refined in the following order:

architecture-overview.md
        |
        v
communication.md
        |
        v
data-ownership.md
        |
        v
concurrency-strategy.md
        |
        v
failure-handling.md
        |
        v
security.md

After these architecture decisions are frozen, the project can move to:

API Contracts
      |
      v
Database Design
      |
      v
Project Skeleton
      |
      v
Implementation

No implementation should introduce new service boundaries without updating the architecture documentation first.