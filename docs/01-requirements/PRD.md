
# BookNGo — Product Requirements Document (PRD)

## 1. Document Information

| Field | Value |
|---|---|
| Product Name | BookNGo |
| Product Type | High-Concurrency Entertainment Ticket Booking & Seat Allocation Platform |
| Domain | Entertainment / Cinema Ticketing |
| Primary Users | Customers, Theatre Operators, Platform Administrators |
| Document | Product Requirements Document |
| Version | 1.0 |
| Status | Draft for Review |

---

# 2. Product Overview

## 2.1 Product Name

**BookNGo**

BookNGo is an entertainment ticket booking platform designed to allow customers to discover movies, theatres, and shows, select seats, temporarily hold seats, complete payment, and receive tickets.

The platform is specifically designed around the high-concurrency requirements of entertainment ticket booking, where many users may attempt to reserve the same seats simultaneously during high-demand movie releases.

## 2.2 Product Description

BookNGo provides a centralized ticket booking experience for cinema operations across multiple theatres, screens, shows, and seat categories.

The system separates major business capabilities into independently deployable services while maintaining strict consistency for show-specific seat allocation.

The platform must ensure that a physical seat cannot be successfully allocated to more than one customer for the same show, even when many booking requests arrive concurrently.

---

# 3. Problem Statement

Cinema ticketing systems can experience severe traffic spikes when popular movies become available for booking.

During these periods, many customers may attempt to:

- view the same show,
- select the same seats,
- hold seats,
- make payments,
- and confirm bookings simultaneously.

A system that does not correctly handle concurrency can result in:

- double-booked seats,
- inconsistent seat availability,
- stale seat information,
- seats remaining locked after failed or abandoned bookings,
- duplicate bookings from repeated requests,
- duplicate payment attempts,
- and inconsistent booking/payment states.

CinePass Entertainment therefore requires a scalable ticket booking platform capable of handling high-concurrency booking activity while maintaining correct seat allocation and reliable booking recovery.

BookNGo addresses this problem through service separation, centralized request routing, service discovery, JWT authentication, persistent booking state, server-authoritative seat holds, and concurrency-safe seat allocation.

---

# 4. Product Vision

To provide a reliable and scalable entertainment ticket booking platform where customers can discover shows, select seats, securely complete bookings, and receive tickets without encountering double-booking or inconsistent seat allocation during high traffic.

---

# 5. Product Goals

## 5.1 Primary Goals

BookNGo shall:

1. Allow customers to discover movies, theatres, and available shows.
2. Allow customers to view show-specific seat availability.
3. Allow customers to select seats and create temporary seat holds.
4. Prevent two customers from successfully acquiring the same seat for the same show.
5. Automatically release expired seat holds.
6. Allow customers to complete payment and confirm bookings.
7. Prevent duplicate booking/payment effects caused by repeated requests.
8. Provide recoverable handling for payment and network failures.
9. Secure authenticated operations using JWT.
10. Provide centralized API routing through an API Gateway.
11. Support service discovery through Eureka.
12. Allow theatre operators to manage their theatre-related information.
13. Support cancellation according to theatre/show policy.
14. Provide customers with confirmed booking and ticket information.
15. Support high-concurrency testing under defined load conditions.

---

# 6. Product Scope

## 6.1 In Scope

### Customer-facing capabilities

- User registration/authentication using phone number and OTP.
- JWT-based authentication.
- Movie discovery.
- Theatre discovery.
- Show discovery.
- Show details.
- Seat map and seat availability.
- Seat selection.
- Temporary seat holds.
- Hold expiry.
- Booking creation.
- Payment initiation.
- Payment status handling.
- Booking confirmation.
- Ticket generation.
- Booking history.
- Booking cancellation where permitted.
- Refund/reversal status handling.

### Theatre operator capabilities

- Theatre management for assigned theatres.
- Screen management.
- Physical seat management.
- Show management.
- Pricing configuration.
- Cancellation policy configuration.
- Viewing bookings.
- Occupancy information.
- Managing theatre-related information.

### Platform administration

- Movie management.
- Theatre operator management.
- Basic platform oversight.

### Platform capabilities

- API Gateway.
- Eureka service discovery.
- JWT authentication.
- Service-to-service communication.
- Independent business services.
- Database ownership by service.
- Concurrency-safe seat allocation.
- Idempotent critical operations.
- Failure recovery mechanisms.
- Logging and request tracing support.
- Online deployment suitable for the academic project.

---

# 7. Out of Scope

The following are not required for the initial Review-1 implementation:

- Full production-grade payment gateway integration.
- Real SMS provider integration if it introduces cost or unnecessary external dependency.
- Complex refund policy engines.
- Dynamic pricing algorithms.
- Recommendation engines.
- Loyalty/reward systems.
- Advertising systems.
- Social login unless required later.
- Advanced analytics.
- Multi-country tax/payment processing.
- Native Android/iOS applications.
- Advanced event-driven architecture where synchronous communication is sufficient.

For Review 1, OTP and payment behavior may be simulated where necessary while preserving the intended system workflow.

---

# 8. Target Users

## 8.1 Customer

A customer uses BookNGo to discover movies and shows, select seats, make a booking, complete payment, and access tickets.

Primary needs:

- Fast movie/show discovery.
- Accurate seat availability.
- Simple seat selection.
- Reliable booking.
- Secure authentication.
- Clear payment status.
- Protection against double booking.
- Access to booking history and tickets.
- Clear cancellation information.

---

## 8.2 Theatre Operator

A theatre operator manages theatre-specific operational information.

Primary needs:

- Manage assigned theatres.
- Manage screens.
- Manage physical seats.
- Create and manage shows.
- Configure seat pricing.
- Configure cancellation policies.
- View bookings.
- View occupancy information.

Operators should only manage theatres assigned to them.

---

## 8.3 Platform Administrator

The platform administrator performs platform-level management and oversight.

Primary responsibilities include:

- Managing movies.
- Managing theatre operators.
- Managing platform-level information.
- Basic operational oversight.

---

# 9. Core User Journey

The primary customer journey is:

```text
Open BookNGo
      ↓
Discover Movie
      ↓
Select Theatre / Location
      ↓
Select Show
      ↓
View Seat Map
      ↓
Select Seats
      ↓
Proceed to Book
      ↓
Server Creates Seat Hold
      ↓
Payment
      ↓
Payment Result
      ↓
Booking Confirmation
      ↓
Ticket Issued

If the customer does not complete the required process within the allowed time:

Seat Hold
    ↓
Expiry
    ↓
Seat Released
    ↓
Available Again
10. Core Booking Concept
10.1 Physical Seat

A physical seat belongs permanently to a screen.

The ownership relationship is:

Theatre
   ↓
Screen
   ↓
Physical Seat

A physical seat does not itself have a globally permanent AVAILABLE/HELD/BOOKED state.

10.2 Show-Specific Seat Availability

Seat availability is specific to a show.

A show uses a screen, and the seats of that screen become bookable inventory for that particular show.

Conceptually:

Show
  ↓
Show Seat Inventory
  ↓
Physical Seat

Therefore the same physical seat can be:

Show A → BOOKED
Show B → AVAILABLE
Show C → HELD

at the same time because these are different shows.

The critical booking identity is:

(show_id, physical_seat_id)
11. Seat Holding

Seat selection in the frontend does not immediately create a server-side hold.

The hold begins only after the customer confirms the selected seats and proceeds to booking.

Seat Selection
      ↓
Customer clicks Continue / Proceed to Book
      ↓
Server validates requested seats
      ↓
Server creates HOLD
      ↓
Hold timer begins

The server is authoritative for the hold timer.

The frontend timer is only a user-interface representation.

12. Hold Policy
12.1 Normal Hold Duration

A successfully created seat hold remains valid for:

5 minutes

If payment has not been initiated before the hold expires:

HELD → AVAILABLE

The expired hold must not permanently lock the seat.

12.2 Payment Processing Grace Period

If payment is initiated while the normal hold is still valid, the booking can enter:

PAYMENT_PENDING

A bounded payment-processing grace period of:

2 minutes

may then be provided.

This prevents an external payment operation from unnecessarily losing the seat immediately while also preventing indefinite seat locking.

13. Payment Requirements

The payment process must support the following outcomes:

SUCCESS
FAILURE
CANCELLED
TIMEOUT
UNKNOWN

A successful payment should result in booking confirmation when the booking remains valid.

A failed payment must not result in a confirmed booking.

An UNKNOWN or unresolved payment must not automatically trigger another charge.

The system must provide a payment-status/reconciliation path for uncertain payment results.

14. Booking and Payment Recovery

BookNGo must handle the case where:

Payment succeeds
      ↓
Client/network failure
      ↓
Customer does not receive response

The customer must be able to safely retrieve the resulting payment/booking state without causing a duplicate charge or duplicate booking.

Similarly, if payment succeeds but booking confirmation cannot be completed:

PAYMENT_SUCCESS
      ↓
Confirmation failure
      ↓
REFUND_PENDING
      ↓
REFUNDED

The exact external refund completion time must not be promised by the platform.

15. Cancellation

A show may be configured as:

CANCELLABLE

or

NON-CANCELLABLE

For cancellable shows, the theatre can configure a cancellation deadline, such as:

12 hours before show time.
24 hours before show time.

The customer must be shown the applicable cancellation policy.

Cancellation execution belongs to the booking workflow, while payment/refund processing belongs to the payment capability.

Complex refund policy rules are outside the initial Review-1 scope.

16. Pricing

BookNGo initially supports simple seat-category pricing.

Example categories:

REGULAR
PREMIUM
VIP

Theatre/show configuration determines the applicable pricing.

The initial implementation does not require dynamic pricing.

17. High-Concurrency Requirement

High concurrency is the central technical requirement of BookNGo.

The platform must be designed for approximately:

1,000 concurrent booking users/requests under defined load-test conditions.

The critical scenario is multiple users attempting to acquire the same or overlapping seats for the same show.

For a specific:

(show_id, physical_seat_id)

the system must ensure:

Successful allocation ≤ 1
Double booking = 0

The system may reject competing requests with an appropriate unavailable/conflict response.

18. Concurrency Correctness

The seat allocation mechanism must be authoritative at the Booking Service/database layer.

Correctness must not depend on:

frontend state,
frontend timers,
browser behavior,
in-memory locks,
or a single application instance.

The system must use persistent transactional mechanisms and database constraints/locking strategies appropriate for concurrent seat allocation.

19. Real-Time Seat Availability

Customers should be able to observe changes to seat availability.

For example:

Customer A holds Seat A10
             ↓
Customer B's interface
             ↓
Seat A10 becomes HELD/unavailable

The system may use real-time update mechanisms for visibility.

However:

Real-time updates are a visibility mechanism, not the correctness mechanism.

A race can still occur between displaying availability and attempting to acquire the seat. The server must therefore revalidate the seat during the actual hold/booking operation.

20. Authentication and Authorization

BookNGo requires authentication for protected customer and operator operations.

The initial authentication mechanism is:

Phone Number
      ↓
OTP
      ↓
OTP Verification
      ↓
JWT

JWT is then used to authenticate subsequent protected API requests.

Roles include:

CUSTOMER
THEATRE_OPERATOR
ADMIN

Authorization must also consider resource ownership.

For example, a theatre operator should not be able to modify a theatre that is not assigned to that operator.

21. Service-Oriented Product Structure

BookNGo is designed as a microservices-based platform.

The current proposed business service boundaries are:

1. User & Identity Service
2. Movie Service
3. Theatre Service
4. Show Service
5. Booking Service
6. Payment Service

Infrastructure components:

7. Eureka Server
8. API Gateway

These service boundaries are derived from product capabilities and requirements and will be validated against the SRS and data model before implementation.

22. Product Capability Ownership
Capability	Responsible Service
User accounts	User & Identity
OTP authentication	User & Identity
JWT authentication	User & Identity / security layer
Movies	Movie
Theatres	Theatre
Screens	Theatre
Physical seats	Theatre
Theatre operators	Theatre / Admin capability
Shows	Show
Pricing	Show
Cancellation policy	Show
Show-seat inventory	Booking
Seat availability	Booking
Seat holds	Booking
Bookings	Booking
Tickets	Booking
Booking cancellation execution	Booking
Payment processing	Payment
Payment status	Payment
Refund/reversal	Payment
Payment reconciliation	Payment
API routing	API Gateway
Service discovery	Eureka
23. Data Ownership Principle

Each business microservice owns its persistent data.

A service must not directly access another service's database.

Cross-service relationships are represented using identifiers/references rather than cross-database foreign keys.

The important distinction is:

Theatre Service
    owns Physical Seat

Booking Service
    owns Show Seat Inventory

This allows physical seats to remain theatre/screen data while their availability is managed independently for each show.

24. Reliability Requirements

The system must fail safely.

Examples:

Client disconnect

If a customer disconnects while holding seats, the seats must eventually become available after the server-side hold expires.

Payment failure

A failed payment must not create a confirmed booking.

Payment uncertainty

An uncertain payment must not automatically trigger another charge.

Duplicate request

Retrying a critical request must not create duplicate effects.

Service failure

A service failure must not permanently lock seats.

Booking confirmation failure

A payment that succeeded while booking confirmation failed must enter a recoverable refund/reversal path.

25. Idempotency

Critical operations must support idempotent behavior.

This includes, where applicable:

Seat hold creation.
Booking creation.
Payment initiation.
Payment callbacks/webhooks.
Payment reconciliation.

Example:

Customer clicks Pay
      ↓
Payment request succeeds
      ↓
Network response is lost
      ↓
Customer retries
      ↓
System recognizes the same operation
      ↓
No duplicate payment
26. Performance and Scalability Goals

BookNGo should support independent scaling of business services.

The architecture should allow high-demand services, especially the Booking Service, to scale independently.

The system should avoid introducing unnecessary synchronous dependencies that could become bottlenecks during traffic spikes.

The high-concurrency booking path must prioritize correctness while remaining responsive under the defined load-test conditions.

No arbitrary production latency target is assumed at this stage unless established through later testing requirements.

27. Security Goals

BookNGo must:

Authenticate protected users.
Use JWT for authenticated API access.
Enforce role-based authorization.
Validate incoming data.
Protect OTP operations through rate limiting.
Secure sensitive configuration/secrets.
Avoid exposing sensitive information in logs.
Protect payment callback handling.
Prevent unauthorized access to theatre/operator resources.
Use HTTPS in deployed environments.
Treat frontend input as untrusted.
28. Deployment Goals

The project should be deployable online with a target of:

₹0 cost for the academic project.

Where external paid services would otherwise be required, Review-1 may use controlled simulation for:

OTP delivery.
Payment processing.
Payment callbacks.

The final deployment strategy will be documented separately after the architecture and implementation requirements are finalized.

29. Product Success Criteria

BookNGo will be considered functionally successful when the system can demonstrate:

A customer can discover a movie.
A customer can find a theatre/show.
A customer can view show-specific seat availability.
A customer can select seats.
The server can create a temporary hold.
Held seats become unavailable to competing customers.
Expired holds are released.
A successful payment can result in booking confirmation.
A failed payment cannot confirm a booking.
A customer can retrieve the booking/ticket.
Duplicate requests do not create duplicate bookings/payments.
Cancellation follows the configured show policy.
Two concurrent users cannot successfully acquire the same (show, physical seat).
Service/database failures do not permanently lock seats.
JWT authentication and authorization work for protected operations.
Requests are routed through the API Gateway.
Services register and discover each other through Eureka.
The system can be tested under the defined high-concurrency scenario.
30. Review-1 Demonstration Focus

The Review-1 implementation should prioritize demonstrating the central engineering problem rather than implementing every possible entertainment-platform feature.

The recommended demonstration path is:

Login / JWT
    ↓
Movie Discovery
    ↓
Theatre + Show
    ↓
Seat Map
    ↓
Select Seats
    ↓
Create Hold
    ↓
Concurrent Seat Attempt
    ↓
Only One Successful Allocation
    ↓
Payment Simulation
    ↓
Booking Confirmation
    ↓
Ticket

Supporting demonstrations should include:

Hold Expiry
Payment Failure
Duplicate Request
Cancellation Policy
Service Discovery
API Gateway
31. Product Constraints

The initial project is subject to the following constraints:

The project is an academic microservices implementation.
The system must demonstrate actual microservice separation.
The sample services from the problem statement are reference capabilities, not mandatory final boundaries.
Additional services may be introduced when justified by business responsibility.
Unnecessary service fragmentation should be avoided.
Each business service should have clear ownership.
Cross-service database access is prohibited.
Booking correctness must be maintained under concurrency.
The implementation should remain achievable within the project schedule.
Review-1 should avoid unnecessary dependence on paid external services.
32. Assumptions

The current product definition assumes:

Cinema is the initial entertainment domain.
A theatre contains one or more screens.
A screen contains physical seats.
A show uses a specific screen.
Show-specific seat inventory is maintained separately from physical seat definitions.
Customers can manually select a location if browser geolocation is unavailable or denied.
Browser geolocation is a convenience feature rather than a mandatory requirement.
Payment and OTP may be simulated for Review-1.
The server is authoritative for seat state and hold expiration.
External payment processing may introduce uncertain outcomes.
Complex recommendation, loyalty, analytics, and dynamic pricing features are not required initially.
33. Future Extensibility

The architecture should leave room for future capabilities without requiring the initial system to implement them.

Potential future extensions include:

Multiple entertainment categories.
Events and concerts.
Advanced payment providers.
Real SMS/OTP providers.
Notifications.
Recommendation systems.
Dynamic pricing.
Loyalty programs.
Analytics.
Event-driven integrations.
Advanced refund policies.
Mobile applications.

These possibilities must not complicate the Review-1 implementation unless explicitly brought into scope.

34. Product Principles

The following principles guide BookNGo:

Correctness over optimistic UI

The server is authoritative for seat allocation.

Business ownership over technical fragmentation

A microservice should exist because it owns a meaningful business capability.

Persistent state over in-memory correctness

Critical booking state must survive application-instance failure.

Safe retries

Retries must not create duplicate booking or payment effects.

Bounded resource locking

Seats must never remain locked indefinitely.

Independent service ownership

Services own their data and business responsibilities.

Recoverability

Uncertain payment and partial failures must have recoverable states.

Simple first implementation

Review-1 should demonstrate the core engineering challenge without unnecessary infrastructure complexity.

35. PRD Completion Status

The PRD establishes the product-level requirements and boundaries for BookNGo.

The following documents should refine these requirements without contradicting them:

SRS
  ↓
User Stories / Use Cases
  ↓
Acceptance Criteria
  ↓
Service Decomposition
  ↓
Architecture
  ↓
ERD
  ↓
API Contracts

Any architectural or implementation decision that changes a product requirement must be explicitly reviewed rather than silently introduced during development.
