# Problem Statement

## PS003 — High-Concurrency Entertainment Ticketing & Seat Allocation Engine

CinePass Entertainment operates a multi-screen cinema ticketing platform that
must support movie discovery, show scheduling, seat selection, temporary seat
holding, booking, payment, and ticket generation.

The platform faces severe traffic spikes during the release of popular movies.
During these periods, many customers may attempt to acquire the same seats for
the same show at approximately the same time.

The primary technical challenge is therefore not ordinary CRUD functionality.
The system must maintain correct seat inventory under concurrent requests.

### Core Problem

For a particular show and physical seat:

- Multiple customers may request the seat concurrently.
- Only one customer may successfully acquire an active hold.
- A confirmed booking must never be created for the same seat for two
  customers.
- Temporary holds must expire automatically.
- A client disconnect must not permanently lock a seat.
- Payment failures must not create confirmed bookings.
- Payment success must remain recoverable even when a client or network
  failure occurs.
- Retried requests must not unintentionally create duplicate bookings or
  payments.

The system therefore requires controlled concurrency, strong consistency for
critical seat allocation, safe failure handling, idempotent operations, secure
authentication, centralized API routing, and service discovery.

## Required Platform Capabilities

The system shall provide:

1. Movie discovery and movie information.
2. Location-based theatre/show discovery.
3. Theatre and screen management.
4. Physical seat configuration.
5. Show scheduling.
6. Show-specific seat availability.
7. Temporary seat holding.
8. Concurrent seat allocation protection.
9. Customer authentication using phone number and OTP.
10. Booking creation and management.
11. Payment processing through a real or simulated provider.
12. Payment failure, timeout, and unknown-state handling.
13. Digital ticket generation.
14. Booking cancellation according to show policy.
15. Refund/reversal handling.
16. Theatre occupancy and booking visibility.
17. JWT-based API security.
18. API Gateway-based centralized routing.
19. Eureka-based service discovery and load balancing.

## Primary Technical Challenge

The key correctness boundary is:

(show_id, physical_seat_id)

A physical seat belongs to a screen, but its availability is specific to a
particular show using that screen.

The system must ensure that concurrent operations on the same show-seat do not
produce conflicting inventory states.

## Target Workload

The project targets approximately:

**1,000 concurrent users/booking requests**

under defined load-test conditions.

The concurrency target is an engineering test target, not a claim of
production-scale capacity.

## Cost Constraint

The project shall target:

**₹0 total project cost**

Free-tier infrastructure, simulated payment, and simulated OTP delivery may
be used where external providers would introduce unavoidable cost or
implementation dependency.

## Review 1 Scope
Skill Development Project

PS003: High-Concurrency Entertainment Ticketing & Seat Allocation Engine

Business Use Case

The company CinePass Entertainment requires a high-concurrency ticket booking engine to support multi-screen cinema operations and scheduled movie showtimes. CinePass faces severe traffic spikes during blockbuster movie releases and requires strict concurrency management to eliminate double-booking and maintain real-time seat availability across screens. The company's technical strategy isolates movie management, show scheduling, user accounts, and booking workflows into distinct microservices. CinePass requires JWT authentication for all user bookings, an API Gateway for centralized request routing, and Eureka service discovery to load-balance traffic across active service nodes.

Tasks

Design movie, show, and booking services

Implement APIs for movie and show management

Develop ticket booking logic with seat allocation

Handle concurrent booking scenarios

Secure APIs using JWT

Use API Gateway for routing

Register services in Eureka

Implement integration testing

Deploy and test booking flow

Sample Microservices

Movie Service

Show Service

Booking Service

User Service

The Review 1 implementation shall prioritize:

- Correct service decomposition.
- Microservice communication.
- Eureka service discovery.
- API Gateway.
- JWT authentication.
- Movie/show discovery.
- Seat availability.
- Seat holding.
- Concurrency protection.
- Booking workflow.
- Basic payment simulation.
- Basic ticket generation.
- Demonstrable failure/recovery behavior.

Advanced production features are outside the initial Review 1 scope.