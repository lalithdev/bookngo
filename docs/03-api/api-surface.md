# BookNGo API Surface

## 1. Purpose

This document defines the initial API surface for BookNGo before the detailed OpenAPI contract is implemented.

The API is organized according to the six business microservices:

1. User Service
2. Movie Service
3. Theatre Service
4. Show Service
5. Booking Service
6. Payment Service

Infrastructure components:

7. API Gateway
8. Eureka Server

The detailed request/response schemas, validation rules, HTTP status codes, and security requirements will be finalized in `openapi.yaml`.

---

## 2. API Entry Point

The frontend communicates with the backend through the API Gateway.

Conceptually:

```text
React Frontend
      ↓
API Gateway
      ↓
Business Microservices

The frontend should not directly depend on internal service hostnames or ports.

3. API Versioning

The public API uses a version prefix:

/api/v1

Example:

GET /api/v1/movies

This provides a stable public API boundary and allows future API versions without immediately breaking existing clients.

4. User Service

Base path:

/api/v1/auth
/api/v1/users
Authentication
Request OTP
POST /api/v1/auth/otp/request

Purpose:

Request an OTP for the supplied phone number.

Authentication:

Public
Verify OTP
POST /api/v1/auth/otp/verify

Purpose:

Verify the OTP and authenticate the user (CUSTOMER role).

Authentication:

Public

Successful verification returns a JWT.

Password Login
POST /api/v1/auth/login

Purpose:

Username/password authentication for THEATRE_OPERATOR and ADMIN roles.

Authentication:

Public

Successful authentication returns a JWT.

Get Current User
GET /api/v1/users/me

Authentication:

JWT required

Purpose:

Retrieve the authenticated user's profile.

Update Current User
PUT /api/v1/users/me

Authentication:

JWT required

Purpose:

Update permitted profile information.

5. Movie Service

Base path:

/api/v1/movies
Public Operations
List Movies
GET /api/v1/movies

Purpose:

Browse movies.

Possible filters:

title
language
genre
status
Get Movie
GET /api/v1/movies/{movieId}

Purpose:

Retrieve details for a specific movie.

Admin Operations
Create Movie
POST /api/v1/movies

Authorization:

ADMIN
Update Movie
PUT /api/v1/movies/{movieId}

Authorization:

ADMIN
Delete/Deactivate Movie
DELETE /api/v1/movies/{movieId}

Authorization:

ADMIN

Implementation may use soft deletion/deactivation where required.

6. Theatre Service

Base path:

/api/v1/theatres

The Theatre Service owns:

Theatre
Screen
Physical Seat
Theatre operator assignment
Public Operations
List Theatres
GET /api/v1/theatres

Possible filters:

location
city
Get Theatre
GET /api/v1/theatres/{theatreId}
Get Screens
GET /api/v1/theatres/{theatreId}/screens
Get Physical Seats
GET /api/v1/screens/{screenId}/seats

The returned seats describe the physical screen configuration.

They do not represent current show-specific availability.

Theatre Operator Operations
Create Theatre
POST /api/v1/theatres

Authorization:

THEATRE_OPERATOR
Update Theatre
PUT /api/v1/theatres/{theatreId}

Authorization:

THEATRE_OPERATOR

The service must verify operator ownership/assignment.

Create Screen
POST /api/v1/theatres/{theatreId}/screens

Authorization:

THEATRE_OPERATOR
Update Screen
PUT /api/v1/screens/{screenId}

Authorization:

THEATRE_OPERATOR
Create Physical Seat
POST /api/v1/screens/{screenId}/seats

Authorization:

THEATRE_OPERATOR
Update Physical Seat
PUT /api/v1/seats/{seatId}

Authorization:

THEATRE_OPERATOR
7. Show Service

Base path:

/api/v1/shows

The Show Service owns:

Show schedule
Movie/show association
Theatre/screen reference
Pricing
Cancellation policy
Public Operations
Search Shows
GET /api/v1/shows

Possible filters:

movieId
theatreId
city
date
language
format
Get Show
GET /api/v1/shows/{showId}
Get Show Pricing
GET /api/v1/shows/{showId}/pricing
Get Cancellation Policy
GET /api/v1/shows/{showId}/cancellation-policy
Theatre Operator Operations
Create Show
POST /api/v1/shows

Authorization:

THEATRE_OPERATOR

The operator must be authorized to manage the selected theatre.

Update Show
PUT /api/v1/shows/{showId}

Authorization:

THEATRE_OPERATOR
Cancel Show
POST /api/v1/shows/{showId}/cancel

Authorization:

THEATRE_OPERATOR
Configure Pricing
PUT /api/v1/shows/{showId}/pricing

Authorization:

THEATRE_OPERATOR
Configure Cancellation Policy
PUT /api/v1/shows/{showId}/cancellation-policy

Authorization:

THEATRE_OPERATOR
8. Booking Service

Base path:

/api/v1/bookings
/api/v1/shows/{showId}/seat-inventory

This is the most concurrency-sensitive API group.

Seat Availability
Get Show Seat Inventory
GET /api/v1/shows/{showId}/seat-inventory

Authentication:

Public or authenticated according to final frontend requirement

Purpose:

Return show-specific seat states.

Possible states:

AVAILABLE
HELD
BOOKED
9. Seat Hold
Create Hold
POST /api/v1/shows/{showId}/holds

Authentication:

JWT required

Purpose:

Attempt to atomically hold one or more seats.

Maximum seats:

6

The request must include an idempotency key.

Example:

Idempotency-Key: <unique-operation-key>

The server creates the hold only after successful concurrency validation.

Get Hold
GET /api/v1/holds/{holdId}

Authentication:

JWT required

Purpose:

Retrieve the current hold state.

The user must own the hold or have an authorized role.

Release Hold
DELETE /api/v1/holds/{holdId}

Authentication:

JWT required

Purpose:

Allow an eligible hold to be released before expiry.

The server remains authoritative for expiration.

10. Booking Creation
Create Booking
POST /api/v1/bookings

Authentication:

JWT required

Purpose:

Create the booking associated with the user's valid hold.

The request must use idempotency protection.

Get Booking
GET /api/v1/bookings/{bookingId}

Authentication:

JWT required

The customer may access their own booking.

Authorized operators/admins may access bookings according to their scope.

List My Bookings
GET /api/v1/bookings/me

Authentication:

JWT required
11. Booking Cancellation
Cancel Booking
POST /api/v1/bookings/{bookingId}/cancel

Authentication:

JWT required

The Booking Service validates:

Booking ownership
Booking state
Show cancellation policy
Cancellation deadline

The Payment Service handles the corresponding refund/reversal state.

12. Ticket
Get Ticket
GET /api/v1/bookings/{bookingId}/ticket

Authentication:

JWT required

A ticket is available only after successful booking confirmation.

13. Payment Service

Base path:

/api/v1/payments

The Payment Service owns:

Payment attempts
Payment state
Payment idempotency
Refunds/reversals
Reconciliation
Initiate Payment
POST /api/v1/payments

Authentication:

JWT required

The request must contain an idempotency key.

Payment initiation is allowed only for an eligible booking/hold state.

Get Payment
GET /api/v1/payments/{paymentId}

Authentication:

JWT required

The customer may access only payments associated with their authorized booking.

Payment Status
GET /api/v1/payments/{paymentId}/status

Authentication:

JWT required

Purpose:

Retrieve the current payment state.

Possible states include:

INITIATED
PROCESSING
SUCCESS
FAILURE
CANCELLED
TIMEOUT
UNKNOWN
REFUND_PENDING
REFUNDED

The exact state model will be finalized in the detailed API and domain contracts.

14. Payment Callback
Payment Provider Callback
POST /api/v1/payments/provider/callback

This endpoint is intended for the payment provider rather than normal frontend usage.

The Payment Service must validate the authenticity of the callback according to the provider integration.

Callback processing must be idempotent.

15. Refund
Get Refund Status
GET /api/v1/payments/{paymentId}/refund

Authentication:

JWT required

Purpose:

Retrieve the current refund/reversal state associated with a payment.

16. Theatre Occupancy

Occupancy is a derived operational capability.

The initial API may expose:

GET /api/v1/theatres/{theatreId}/occupancy

Authorization:

THEATRE_OPERATOR

The implementation should derive occupancy from authoritative booking/inventory data rather than introducing a separate Occupancy Service.

17. Admin APIs

Review 1 requires only limited administration.

Potential endpoints:

POST   /api/v1/movies
PUT    /api/v1/movies/{movieId}
DELETE /api/v1/movies/{movieId}

POST   /api/v1/theatre-operators
GET    /api/v1/theatre-operators
PUT    /api/v1/theatre-operators/{operatorId}

Exact administrator endpoints will be finalized after the core API contract.

18. Service-to-Service APIs

Internal service communication is separate from the public frontend API surface.

Examples include:

Booking Service
    ↓
Show Service

Booking Service
    ↓
Theatre Service

Booking Service
    ↓
User Service

Booking Service
    ↓
Payment Service

Internal APIs should expose only the information required by consuming services.

They should not expose direct database access.

19. Idempotency Requirements

The following operations require idempotency protection:

POST /api/v1/shows/{showId}/holds

POST /api/v1/bookings

POST /api/v1/payments

POST /api/v1/payments/provider/callback

The detailed API contract will define the exact header and behavior.

20. API Security Summary
API Group	Customer	Theatre Operator	Admin
Movie discovery	✓	✓	✓
Movie management	—	—	✓
Theatre discovery	✓	✓	✓
Theatre management	—	Assigned only	✓
Show discovery	✓	✓	✓
Show management	—	Assigned only	✓
Seat availability	✓	✓	✓
Seat hold	✓	—	✓/operational
Booking	✓	Operational access	✓
Own booking history	✓	—	✓
Booking cancellation	✓ own	Operational	✓
Payment	✓ own	—	✓/operational
Refund status	✓ own	—	✓
Occupancy	—	Assigned only	✓

The exact authorization matrix will be reflected in openapi.yaml.

21. HTTP Status Categories

The detailed API contract should use standard HTTP semantics.

Expected categories include:

200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
429 Too Many Requests
500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
504 Gateway Timeout

The exact status code for each operation will be defined in the OpenAPI specification.

22. Concurrency-Specific API Behavior

Seat allocation failures should be represented as a conflict/unavailable condition rather than an internal server failure when the request itself is valid but another user already acquired the seat.

Example:

User A → requests A10
User B → requests A10

User A → success
User B → conflict/unavailable

The client must refresh/retrieve current seat inventory rather than assuming that the request succeeded.

23. API Contract Constraints

The following constraints are frozen:

Public API
    ↓
API Gateway
    ↓
Versioned /api/v1 routes
    ↓
Business Service

The frontend must not:

Access service databases.
Depend on internal service ports.
Directly manipulate seat state.
Treat cached seat data as authoritative.
Create bookings without server-side validation.

The backend must:

Validate authentication.
Validate authorization.
Validate request data.
Validate resource ownership.
Validate business rules.
Enforce concurrency.
Enforce idempotency for critical operations.
