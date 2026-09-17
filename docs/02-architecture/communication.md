# BookNGo — Service Communication Architecture

## 1. Purpose

This document defines how the BookNGo microservices communicate with each other and with external systems.

It establishes:

- Synchronous communication.
- Service-to-service REST communication.
- API Gateway routing.
- Eureka-based service discovery.
- Request flow between services.
- Communication ownership.
- Error propagation expectations.
- Idempotent operations.
- Where asynchronous communication may be introduced.
- Communication rules that prevent tight coupling.

Detailed endpoint definitions are intentionally excluded from this document and will be defined later in the API contract.

---

# 2. Communication Principles

BookNGo follows these communication principles.

## 2.1 API-Based Communication

Services communicate through defined APIs.

A service must not access another service's database directly.

```text
Service A
    |
    | API
    v
Service B
    |
    v
Service B Database

The following is prohibited:

Service A
    |
    X
    |
    +----> Service B Database
2.2 Synchronous Communication by Default

BookNGo will primarily use synchronous REST communication for operations where the caller requires an immediate response.

Examples:

Authentication.
Movie retrieval.
Theatre retrieval.
Show retrieval.
Seat availability.
Seat hold.
Booking operations.
Payment initiation.
Payment status queries.

This keeps the initial implementation understandable and suitable for the academic project.

2.3 Asynchronous Communication Where Useful

Asynchronous communication may be introduced for operations that do not require an immediate response.

Potential examples include:

Booking confirmation notifications.
Ticket notification.
Audit events.
Payment reconciliation events.
Analytics/occupancy updates.

Asynchronous messaging is not mandatory for the initial architecture.

It should only be introduced where it provides a clear architectural benefit.

3. Communication Topology

The logical communication topology is:

                         +----------------+
                         |    Frontend    |
                         +-------+--------+
                                 |
                                 | HTTPS
                                 v
                         +----------------+
                         |  API Gateway   |
                         +-------+--------+
                                 |
                    +------------+------------+
                    |            |            |
                    v            v            v
              User & Identity  Movie      Theatre
                    |          Service      Service
                    |            |            |
                    |            |            |
                    +------------+------------+
                                 |
                                 v
                            Show Service
                                 |
                                 v
                           Booking Service
                                 |
                                 v
                           Payment Service

The actual request path depends on the operation.

Not every service communicates directly with every other service.

Communication should follow the ownership boundaries defined in:

docs/02-architecture/service-boundaries.md
4. External Client Communication

The frontend communicates with the backend through the API Gateway.

React Frontend
      |
      | HTTPS
      v
API Gateway
      |
      v
Backend Service

The frontend should not normally call internal microservice addresses directly.

For example:

Correct:

Frontend
   |
   v
API Gateway
   |
   v
Booking Service

Instead of:

Frontend
   |
   X
   |
   +----> Booking Service directly

This provides a single external API entry point.

5. API Gateway Communication

The API Gateway routes external requests to the appropriate service.

Conceptually:

/api/auth/**       -> User & Identity Service

/api/movies/**     -> Movie Service

/api/theatres/**   -> Theatre Service

/api/shows/**      -> Show Service

/api/bookings/**   -> Booking Service

/api/payments/**   -> Payment Service

The exact route definitions will be finalized in the API design stage.

The Gateway should use service discovery rather than hardcoding deployment-specific service addresses where appropriate.

6. Eureka-Based Service Discovery

Eureka provides service registration and discovery.

At startup:

User & Identity Service
        |
        v
     Eureka

Movie Service
        |
        v
     Eureka

Theatre Service
        |
        v
     Eureka

Show Service
        |
        v
     Eureka

Booking Service
        |
        v
     Eureka

Payment Service
        |
        v
     Eureka

The services register their logical service names with Eureka.

Examples of logical service identifiers:

USER-SERVICE
MOVIE-SERVICE
THEATRE-SERVICE
SHOW-SERVICE
BOOKING-SERVICE
PAYMENT-SERVICE

The final naming convention will be kept consistent across configuration, Gateway routes, and service-to-service clients.

7. Gateway to Service Communication

The normal external request path is:

Client
   |
   v
API Gateway
   |
   | Service Discovery
   v
Target Service

For example:

GET /api/movies/123
        |
        v
API Gateway
        |
        v
MOVIE-SERVICE

The Gateway does not implement movie business logic.

It only routes the request to the service responsible for that capability.

8. User & Identity Communication
8.1 Authentication

Authentication is handled by User & Identity Service.

Frontend
    |
    v
API Gateway
    |
    v
User & Identity Service
    |
    v
User Database

The service verifies the supplied authentication information and issues a JWT after successful authentication.

8.2 Protected Requests

After authentication:

Frontend
    |
    | JWT
    v
API Gateway
    |
    v
Target Service

The JWT carries identity/authorization information required for protected operations.

Detailed JWT validation and authorization responsibilities are defined in:

docs/02-architecture/security.md
9. Movie Service Communication

Movie information is primarily requested through the Gateway.

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

Other services may request movie information when required through the Movie Service API.

The Movie Service remains the authoritative source for movie metadata.

10. Theatre Service Communication

The Theatre Service provides information about:

Theatres.
Locations.
Screens.
Physical seats.
Theatre operators.

A typical request:

Frontend
    |
    v
API Gateway
    |
    v
Theatre Service
    |
    v
Theatre Database

Other services must obtain required theatre information through an API rather than directly querying the Theatre Database.

11. Show Service Communication

The Show Service is responsible for scheduled shows.

A show references entities owned by other services.

Conceptually:

Show
 |
 +---- movie_id
 |
 +---- theatre_id
 |
 +---- screen_id

These identifiers do not mean that Show Service owns those entities.

The authoritative owners remain:

movie_id
    -> Movie Service

theatre_id
    -> Theatre Service

screen_id
    -> Theatre Service
12. Show Discovery Communication

A show discovery request may require information from multiple domains.

A possible synchronous flow is:

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
    +----> Movie Service
    |
    +----> Theatre Service
    |
    v
Response

The exact optimization of this flow will be determined later.

The initial implementation should prioritize clear ownership and correctness over premature optimization.

13. Booking Service Communication

Booking Service is the central service for booking transactions.

It may need information owned by:

User & Identity Service.
Show Service.
Theatre Service.
Payment Service.

However, it does not own those domains.

Conceptually:

                 +----------------+
                 | User & Identity|
                 +-------+--------+
                         |
                         |
+------------+           v
| Show       | ---> Booking Service <--- Payment Service
| Service    |           |
+------------+           |
                         v
                  Booking Database

The exact dependency direction must remain controlled to prevent circular service dependencies.

14. Seat Availability Communication

Seat availability is owned by Booking Service.

The frontend requests availability through:

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

The Booking Service may use Show and Theatre information to construct the appropriate show-seat inventory context.

However, the current seat state remains owned by Booking Service.

15. Seat Hold Communication

Seat holding is a synchronous operation because the user needs an immediate result.

Flow:

Frontend
    |
    | POST hold request
    v
API Gateway
    |
    v
Booking Service
    |
    | Validate request
    |
    | Check show
    |
    | Attempt seat allocation
    |
    v
Booking Database
    |
    v
Hold Result

Possible results include:

SUCCESS

or:

SEAT_UNAVAILABLE

The server determines whether the hold was successfully acquired.

The frontend must not assume success until it receives the server response.

16. High-Concurrency Communication

Multiple clients may send requests simultaneously.

Example:

User A ----+
User B ----+
User C ----+
User D ----+----> API Gateway ----> Booking Service
User E ----+

The Gateway forwards requests but does not determine which user gets the seat.

The Booking Service and its persistence/concurrency mechanism determine the outcome.

For:

Show 101 + Seat A10

only one concurrent operation may successfully acquire the seat.

The detailed locking/atomicity strategy belongs in:

docs/02-architecture/concurrency-strategy.md
17. Booking-to-Payment Communication

Payment initiation is a synchronous interaction from the booking workflow.

Conceptually:

Booking Service
       |
       | Payment Request
       v
Payment Service
       |
       v
Payment Database

The Payment Service processes the payment operation and returns an immediate status where possible.

Possible payment outcomes include:

SUCCESS
FAILURE
CANCELLED
TIMEOUT
UNKNOWN

The Booking Service uses the payment result to determine the appropriate booking state.

18. Payment Status Communication

When a payment result is uncertain, the system must support payment status lookup/reconciliation.

Conceptually:

Booking Service
       |
       | Query Payment Status
       v
Payment Service
       |
       v
Payment Provider / Payment Database

This prevents the system from assuming that an unknown result means payment definitely failed.

It also prevents an uncontrolled duplicate payment attempt.

19. Payment Provider Communication

The Payment Service acts as the boundary between BookNGo and an external payment provider.

Booking Service
       |
       v
Payment Service
       |
       v
External Payment Provider

The external provider is never called directly by Booking Service.

This keeps payment-provider-specific logic inside Payment Service.

For Review 1, the external provider may be simulated.

The service boundary should remain the same even when a simulated provider is used.

20. Refund Communication

When a confirmed booking is cancelled according to the applicable policy, the workflow may involve:

Booking Service
       |
       | Refund Request
       v
Payment Service
       |
       v
Payment Provider

The Booking Service owns the booking cancellation state.

The Payment Service owns the financial refund/reversal state.

21. Cancellation Communication

Cancellation involves multiple responsibilities.

Client
   |
   v
API Gateway
   |
   v
Booking Service
   |
   +----> Show Service
   |       |
   |       +----> Cancellation Policy
   |
   v
Booking state updated
   |
   v
Payment Service
   |
   v
Refund/Reversal

The exact flow may be optimized later.

The ownership remains:

Show Service
    -> cancellation policy

Booking Service
    -> cancellation decision/execution

Payment Service
    -> refund/reversal
22. Service-to-Service Communication Style

The initial implementation will use:

HTTP/REST

for synchronous service-to-service communication.

Spring Boot services may use a declarative HTTP client such as Spring Cloud OpenFeign where appropriate.

Conceptually:

Booking Service
      |
      | REST client
      v
Show Service

and:

Booking Service
      |
      | REST client
      v
Payment Service

The selected client mechanism should remain consistent across the project.

23. Avoiding Circular Dependencies

Service dependencies should be designed carefully.

An undesirable dependency cycle would be:

Booking Service
      |
      v
Payment Service
      |
      v
Booking Service

This can create difficult failure and transaction behavior.

Where callbacks are required, the interaction should use clearly defined contracts and state transitions rather than unrestricted bidirectional synchronous calls.

The detailed booking/payment interaction will be finalized before implementation.

24. Synchronous vs Asynchronous Decision Matrix
Operation	Initial Communication
User authentication	Synchronous REST
OTP verification	Synchronous REST
Movie retrieval	Synchronous REST
Theatre retrieval	Synchronous REST
Show retrieval	Synchronous REST
Seat availability	Synchronous REST
Seat hold	Synchronous REST
Booking creation	Synchronous REST
Payment initiation	Synchronous REST
Payment status	Synchronous REST
Booking cancellation	Synchronous REST
Refund request	Synchronous REST initially
Ticket retrieval	Synchronous REST
Notification	Asynchronous candidate
Audit events	Asynchronous candidate
Analytics	Asynchronous candidate
Payment reconciliation events	Asynchronous candidate

The initial implementation should avoid introducing a message broker unless a concrete requirement justifies it.

25. Communication During Booking

The complete conceptual communication flow is:

                         CUSTOMER
                            |
                            v
                     React Frontend
                            |
                            v
                      API Gateway
                            |
                            v
                     Booking Service
                            |
                  +---------+---------+
                  |                   |
                  v                   v
             Show Service       Theatre Service
                  |                   |
                  +---------+---------+
                            |
                            v
                     Booking Database
                            |
                            | Payment Initiation
                            v
                     Payment Service
                            |
                            v
                     Payment Database
                            |
                            v
                    Payment Provider

The exact call sequence will be refined during API and workflow design.

26. Request Correlation

Each request should be traceable across service boundaries.

A correlation/request identifier should be propagated through service-to-service calls.

Conceptually:

Frontend
   |
   | Request ID: ABC123
   v
API Gateway
   |
   | Request ID: ABC123
   v
Booking Service
   |
   | Request ID: ABC123
   v
Payment Service

This allows logs from multiple services to be associated with the same business operation.

This is particularly important when debugging concurrent booking and payment failures.

27. Timeout Expectations

Synchronous service calls must use bounded timeouts.

A service must not wait indefinitely for another service.

Conceptually:

Service A
    |
    | Request
    v
Service B
    |
    | No response
    |
    v
Timeout

After timeout, Service A must execute the appropriate failure/recovery logic.

Timeout values will be configured during implementation and deployment based on the actual environment.

They should not be treated as business rules.

28. Retry Expectations

Retries must be used carefully.

Retries are appropriate only when the operation is safe to retry.

Examples of operations requiring idempotency before retry:

Booking creation.
Payment initiation.
Payment callbacks.
Cancellation.

A retry must not transform:

one logical operation

into:

multiple bookings

or:

multiple charges

Idempotency keys and business identifiers will be defined in the API contract.

29. Error Propagation

A service should return a meaningful business/technical error to its caller.

Example:

Payment Service
       |
       | PAYMENT_UNKNOWN
       v
Booking Service
       |
       | Booking remains recoverable
       v
Frontend
       |
       v
User informed that payment status requires confirmation

Internal implementation details such as stack traces must not be exposed to users.

30. Service Availability and Degraded Operation

Not every service failure should cause the entire system to fail.

For example:

Movie Service unavailable

Movie discovery may fail, but existing authentication data should remain intact.

Payment Service unavailable

New payment-dependent bookings may be unable to complete, but browsing should remain available.

Booking Service unavailable

Seat allocation and booking operations should fail safely rather than producing uncertain seat ownership.

The detailed failure strategy will be defined in:

docs/02-architecture/failure-handling.md
31. Communication Security

External communication:

Frontend <-> API Gateway

should use HTTPS in deployed environments.

Internal service communication should also be protected appropriately in deployment.

JWT-based authentication is used for protected user operations.

The detailed security architecture will be defined in:

docs/02-architecture/security.md
32. Communication Rules

The following rules are mandatory for implementation.

Rule 1

Frontend communicates with backend through API Gateway.

Rule 2

Services communicate through APIs, not databases.

Rule 3

Eureka is used for service discovery.

Rule 4

REST is the initial synchronous communication mechanism.

Rule 5

Asynchronous messaging is optional and must have a concrete justification.

Rule 6

Booking Service remains authoritative for seat allocation.

Rule 7

Payment Service remains authoritative for payment state.

Rule 8

Service-to-service calls must have bounded timeouts.

Rule 9

Retryable operations must be idempotent.

Rule 10

Circular synchronous dependencies should be avoided.

Rule 11

Correlation/request identifiers should be propagated across service boundaries.

Rule 12

No service may directly access another service's database.

33. Communication Dependency Summary

The primary communication dependencies are:

                         +------------------+
                         |  API Gateway     |
                         +--------+---------+
                                  |
              +-------------------+-------------------+
              |          |          |        |         |
              v          v          v        v         v
            User       Movie      Theatre   Show    Booking
            Service    Service    Service   Service  Service
                                                   |
                                                   v
                                             Payment Service

Additional service-to-service calls may occur when required by the workflow, but they must respect the ownership boundaries.

34. Architectural Decision Summary
Concern	Decision
External entry point	API Gateway
Service discovery	Eureka
Primary synchronous protocol	REST/HTTP
Service database access	Owner service only
Frontend → backend	Gateway
Service discovery mechanism	Eureka
Seat availability	Booking Service
Payment processing boundary	Payment Service
Default communication model	Synchronous
Asynchronous messaging	Optional/future
Retry	Only for idempotent operations
Timeout	Required for synchronous calls
Request tracing	Correlation ID
Internal business logic	Remains inside owning service
Direct database sharing	Prohibited
35. Communication Baseline

The approved initial communication architecture is:

Client
   |
   | HTTPS
   v
API Gateway
   |
   | Eureka-based discovery
   v
Business Services
   |
   | REST service-to-service communication
   v
Other Business Services
   |
   v
Owner Databases

The architecture intentionally starts with synchronous REST communication because it is sufficient for the core BookNGo workflows and keeps the system manageable.

Asynchronous messaging may be introduced later for clearly identified non-blocking workloads without changing the business service boundaries.

The communication design must preserve:

Service ownership.
Data isolation.
Booking correctness.
Payment safety.
Idempotency.
Failure recovery.
Independent deployment.
