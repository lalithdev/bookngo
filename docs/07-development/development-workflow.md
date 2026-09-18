# Development Workflow

## Principle

Do not build microservices randomly.

Implementation must follow the finalized requirements, service boundaries,
data ownership, API contracts, and dependency graph.

AI agents may implement the system, but they must not independently redefine
business requirements or service boundaries during implementation.

---

# Phase 0 — Requirements Baseline

Before writing service code:

1. Freeze product definition.
2. Freeze actors and use cases.
3. Freeze user stories.
4. Freeze functional requirements.
5. Freeze non-functional requirements.
6. Freeze business rules and invariants.
7. Freeze booking state model.
8. Freeze acceptance criteria.

---

# Phase 1 — Service Decomposition

Service decomposition is frozen before implementation.

### Capability Inventory

The analyzed items below are **CAPABILITIES**, not separate microservices:

- Movie
- Location
- Theatre
- Screen
- Physical Seat
- Show
- Pricing
- Seat Availability
- Seat Hold
- Booking
- Payment
- Authentication
- Ticket
- Cancellation
- Refund
- Occupancy

### Mapping of Capabilities to Frozen Microservices

The capabilities map to the six frozen business microservices as follows:

- **Movie** → Movie Service
- **Location** → Theatre Service
- **Theatre** → Theatre Service
- **Screen** → Theatre Service
- **Physical Seat** → Theatre Service
- **Show** → Show Service
- **Pricing** → Show Service
- **Cancellation Policy** → Show Service
- **Seat Availability** → Booking Service
- **Seat Hold** → Booking Service
- **Booking** → Booking Service
- **Ticket** → Booking Service
- **Payment** → Payment Service
- **Refund** → Payment Service
- **Authentication** → User Service
- **Occupancy** → derived/operational capability owned according to the finalized architecture; do not create an Occupancy Service.

Do not change any service boundaries.

### Implementation / Workflow Dependencies

The dependency sequence below describes implementation and workflow execution dependencies, not service boundaries. The six business microservices and two infrastructure services remain the strict service boundaries:

Stage A — Infrastructure Foundation
- Eureka Server
- API Gateway
- Spring Security / JWT foundation

Stage B — Core Domain Services
- User Service
- Movie Service
- Theatre Service
- Show Service

Stage C — Critical Engine & Booking Workflow
- Show Service (Show-specific seat inventory)
- Booking Service (Seat availability, Seat hold, Concurrency, Booking)

Stage D — Transaction Workflow
- Booking Service
- Payment Service (Payment processing, Refund simulation)
- Ticket generation (Booking Service)

Stage E — Supporting Operational Capabilities
- OTP (User Service)
- Profile (User Service)
- Booking History (Booking Service)
- Cancellation Policy (Show Service / Booking Service)
- Theatre Management (Theatre Service)
- Occupancy (derived/operational capability)

Each capability was evaluated for business cohesion, data ownership, consistency, concurrency, scalability, failure isolation, and implementation complexity during decomposition.

Phase 2 — Architecture

Define:

System context.
Service topology.
API Gateway.
Eureka.
Authentication flow.
Service communication.
Synchronous communication.
Asynchronous communication if required.
Failure handling.
Concurrency strategy.
Scalability approach.

No implementation should begin until the architecture is internally
consistent with the service decomposition.

Phase 3 — Data Ownership

For each service define:

Owned entities.
Owned database/schema.
Foreign-reference strategy.
Required indexes.
Unique constraints.
Transaction boundaries.

A service must not directly modify another service's database.

The critical seat inventory boundary must remain:

(show_id, physical_seat_id)

Phase 4 — API Contracts

Define APIs before implementation.

For every API define:

Method.
Path.
Authentication.
Authorization.
Request body.
Response body.
Status codes.
Validation.
Error model.
Idempotency requirements.
Inter-service usage.

OpenAPI becomes the contract for implementation.

Phase 5 — Project Skeleton

Create the repository structure based on finalized services.

Example high-level structure:

BookNGo/
├── frontend/
├── infrastructure/
├── services/
└── docs/

The contents of services/ are strictly limited to the 6 frozen business microservices (`user-service`, `movie-service`, `theatre-service`, `show-service`, `booking-service`, `payment-service`).

Do not create arbitrary or extra services.

Phase 6 — Infrastructure Foundation

Implement:

Eureka Server.
API Gateway.
Shared security conventions.
Configuration strategy.

The infrastructure must support the finalized service topology.

Phase 7 — Service Implementation

Each service follows MVC-style internal structure:

Controller
    ↓
Service
    ↓
Repository
    ↓
Database

Where appropriate:

Controller
    ↓
Application Service
    ↓
Domain Logic
    ↓
Repository

Each service owns its business logic and data.

Phase 8 — Critical Booking Engine

Implement the highest-risk functionality first following implementation workflow dependencies across service boundaries:

Show (Show Service)
  ↓
Show-specific Seat Inventory (Show Service / Booking Service interface)
  ↓
Seat Hold (Booking Service)
  ↓
Concurrency Control (Booking Service)
  ↓
Booking (Booking Service)

*(Note: The above sequence represents implementation/workflow dependencies across Show Service and Booking Service, not separate microservice boundaries).*

Critical rules:

Maximum six seats per booking.
Five-minute normal hold.
Server-side expiry.
One active hold per show-seat.
No double booking.
Idempotent booking.
Safe failure.
Client disconnect does not permanently preserve a hold.

Phase 9 — Payment Flow

Implement transaction workflow dependencies across service boundaries:

Booking (Booking Service)
   ↓
Payment Initiation (Payment Service)
   ↓
Payment Processing (Payment Service)
   ↓
Payment Result (Payment Service)
   ↓
Booking Confirmation (Booking Service)
   ↓
Ticket (Booking Service)

*(Note: The above sequence represents transaction workflow steps executed across Booking Service and Payment Service, not separate microservice boundaries).*

Support:

SUCCESS.
FAILURE.
CANCELLED.
TIMEOUT.
UNKNOWN.

Payment operations must be idempotent.

UNKNOWN must not blindly trigger another payment.

Phase 10 — Recovery

Test:

Client disconnect.
Network failure.
Service timeout.
Duplicate request.
Duplicate callback.
Payment success followed by confirmation failure.
Expired hold.
Payment timeout.

Phase 11 — Frontend

Frontend implementation follows the backend contracts.

Priority screens:

Movie discovery.
Theatre/show selection.
Seat map.
Seat hold/countdown.
Payment.
Booking confirmation.
Ticket.
Booking history.
Basic theatre management.

The frontend countdown is informational.

The backend remains authoritative.

Phase 12 — Testing

Testing must include:

Functional
Movie discovery.
Show discovery.
Seat selection.
Hold.
Booking.
Payment.
Ticket.
Cancellation.
Security
JWT.
Role authorization.
Invalid/expired token.
Unauthorized theatre access.
Concurrency
Same-seat contention.
Overlapping multi-seat requests.
1,000 concurrent requests under defined test conditions.
Failure
Hold expiry.
Client disconnect.
Payment failure.
Payment timeout.
Payment unknown.
Confirmation failure.
Duplicate callback.

Phase 13 — Deployment

Deploy the demonstrable system using zero-cost infrastructure where feasible.

Deployment should preserve:

API Gateway.
Service discovery.
Service routing.
Database connectivity.
Authentication.
Critical booking functionality.

Phase 14 — AI Agent Development Rules

AI agents may:

Implement code from frozen contracts.
Generate boilerplate.
Write tests.
Fix implementation errors.
Improve code quality.
Generate documentation from implemented behavior.

AI agents must not silently:

- create new business services
- move entity ownership
- change business rules
- change hold duration
- change payment grace period
- change state transitions
- change API contracts
- change concurrency guarantees

Any such change requires explicit review against the frozen documents.

