# Security Architecture

## 1. Purpose

This document defines the security architecture for BookNGo.

The primary security objectives are:

- Authenticate customers and authorized users.
- Protect booking and payment operations.
- Use JWT-based authentication for protected APIs.
- Enforce role-based authorization.
- Protect OTP-based authentication.
- Prevent unauthorized access to theatre management operations.
- Keep credentials and sensitive information secure.
- Ensure service-to-service communication does not bypass authorization boundaries.

---

## 2. Security Boundaries

BookNGo has the following major security boundaries:

```text
Customer Browser
       ↓
API Gateway
       ↓
Business Microservices
       ↓
Individual Databases

External Payment Provider
       ↕
Payment Service

OTP/SMS Provider
       ↕
User Service

The frontend is treated as an untrusted client.

All important authorization decisions must happen on the backend.

3. Authentication Model

BookNGo uses role-specific authentication, all resulting in JWT issuance:

CUSTOMER:
Phone Number + OTP → JWT

THEATRE_OPERATOR:
Username + Password → JWT

ADMIN:
Username + Password → JWT

The User Service owns authentication.

The Customer (OTP) authentication flow is:
1. User submits phone number.
2. User Service generates/sends (or simulates) OTP.
3. User submits OTP.
4. User Service validates OTP against `otp_verifications`.
5. User Service authenticates the user.
6. JWT is issued.
7. Client sends JWT with protected requests.

The Operator and Admin (Password) authentication flow is:
1. Operator/Admin submits username (or phone/email) and password.
2. User Service validates credentials against `users.password_hash`.
3. JWT is issued.
4. Client sends JWT with protected requests.
4. JWT Authentication

JWT is the primary authentication mechanism for protected APIs.

A successful authentication produces a signed JWT.

The client sends the token using:

Authorization: Bearer <JWT>

Protected services validate the token before allowing protected operations.

5. JWT Claims

The JWT should contain only the claims required for authorization and request identification.

Typical claims may include:

sub
user_id
role
iat
exp

The exact token structure will be finalized during API/security implementation.

Sensitive information must not be placed inside the JWT.

JWT payloads should not contain:

Passwords
OTP values
Payment credentials
Sensitive personal information
Secrets
6. JWT Expiration

JWTs must have a finite expiration time.

Expired tokens must be rejected.

The system must not accept a JWT indefinitely.

The exact access-token lifetime is an implementation/configuration decision and should be finalized before production deployment.

7. JWT Signing Secret

JWT signing credentials must not be hardcoded into source code.

They should be supplied through environment-specific configuration.

For example:

JWT_SECRET

or an equivalent secure configuration mechanism.

Secrets must not be committed to Git.

8. API Gateway Security

The API Gateway is the public entry point for frontend traffic.

Conceptually:

Frontend
   ↓
API Gateway
   ↓
Services

The Gateway may perform initial authentication/token validation and route requests.

However, individual services must not blindly trust frontend-provided identity information.

Protected services should validate the authenticated context according to the security architecture.

9. Protected APIs

The following operations require authentication:

Seat hold
Booking creation
Payment initiation
Booking history
Ticket access
Cancellation
User profile operations
Theatre operator management operations

Public/read-only operations may include:

Movie discovery
Show discovery
Theatre discovery
Public show information

The final API contract will define the exact security requirement for each endpoint.

10. Authorization

Authentication answers:

Who are you?

Authorization answers:

What are you allowed to do?

BookNGo uses role-based authorization.

Primary roles:

CUSTOMER
THEATRE_OPERATOR
ADMIN
11. Customer Authorization

Customers may perform operations related to their own account and bookings.

Examples:

Browse movies
Browse shows
View seat availability
Hold seats
Create bookings
Initiate payment
View tickets
View booking history
Cancel eligible bookings
View refund status

A customer must not be able to modify another customer's booking.

12. Theatre Operator Authorization

Theatre operators manage only the theatre resources assigned to them.

They may manage:

Theatre
Screens
Physical Seats
Shows
Pricing
Occupancy
Booking-related operational information
Cancellation configuration

An operator must not automatically receive platform-wide administrative privileges.

The backend must verify that the requested theatre belongs to the operator's authorized scope.

13. Admin Authorization

Administrators have platform-level management capabilities.

For Review 1, administrative scope is intentionally limited.

Examples:

Movie management
Theatre operator management
Basic platform oversight

Additional administrative functionality can be introduced later without changing the core architecture.

14. Resource Ownership

Authorization must consider resource ownership.

Example:

Operator A
   ↓
Theatre 1

Operator A may modify Theatre 1.

If Operator A attempts:

PUT /theatres/2

where Theatre 2 belongs to Operator B, the request must be rejected.

Role verification alone is insufficient.

The service must also verify resource ownership/assignment.

15. Booking Ownership

Customers must only access their own protected booking information unless a privileged role has explicit access.

Example:

Customer A
 ↓
Booking A → allowed

Customer A
 ↓
Booking B → rejected

The backend must perform this authorization check.

The frontend must not be trusted to enforce ownership.

16. Payment Authorization

Payment operations are sensitive.

A customer may initiate or inspect payment operations associated with their own booking.

The Payment Service must validate the relationship between:

User
Booking
Payment

A customer must not be able to manipulate another customer's payment by simply changing an ID in the request.

17. OTP Security

OTP authentication must be protected against abuse.

The system should enforce:

OTP expiration
Attempt limits
Request rate limiting
Verification limits
Secure OTP generation
Safe OTP storage/handling

The normal OTP validity period is:

5 minutes

Expired OTPs must be rejected.

18. OTP Storage

OTP values should not be stored unnecessarily in plaintext.

Where persistent storage is required, the implementation should use an appropriate secure representation.

OTP records should contain information such as:

user/phone reference
OTP state
created_at
expires_at
attempt count
verification status

The exact persistence model belongs to the User Service.

19. OTP Rate Limiting

OTP requests and verification attempts must be rate limited.

This prevents uncontrolled repeated attempts.

Examples:

Repeated OTP generation
Repeated incorrect OTP submission
Repeated verification requests

When the allowed limit is exceeded, the request should be rejected for a controlled period.

Exact limits will be configuration values.

20. Password Handling

The primary customer authentication mechanism is OTP.

If passwords are introduced for any administrative/operator account, they must never be stored in plaintext.

Passwords must use a strong one-way password hashing mechanism.

The authentication implementation should rely on Spring Security's supported password-encoding mechanisms rather than custom cryptography.

21. Input Validation

All externally supplied input must be validated on the backend.

Validation applies to:

Phone numbers
OTP values
IDs
Movie information
Show information
Seat selections
Pricing
Booking requests
Payment requests
Cancellation requests

Spring Boot services should use Jakarta Bean Validation where appropriate.

22. Seat Request Validation

Booking requests must validate:

Show exists
Requested seats belong to the show/screen
Requested seats are valid
Maximum seat count is respected
User is authenticated
Request is not duplicated

The configured maximum seat count is:

6 seats per booking
23. SQL Injection Protection

The application must not construct SQL queries by concatenating untrusted user input.

Hibernate/JPA repository mechanisms and parameterized queries should be used.

The database should not be directly exposed to the frontend.

24. Cross-Service Database Access

A service must not directly access another service's database.

For example:

Booking Service
      X
Theatre Database

Instead:

Booking Service
      ↓
Theatre Service API
      ↓
Theatre Database

This preserves data ownership and security boundaries.

25. Database Credentials

Database credentials must be environment-specific.

They must not be committed into the repository.

Configuration should use environment variables or secure deployment configuration.

Example:

DB_URL
DB_USERNAME
DB_PASSWORD
26. CORS

The backend must define an appropriate CORS policy for the deployed frontend.

The system should not use unrestricted CORS in production without justification.

During local development, the frontend development origin may be explicitly allowed.

The exact production frontend origin will be configured during deployment.

27. HTTPS

Production traffic should use HTTPS.

The intended flow is:

Browser
  ↓ HTTPS
API Gateway
  ↓
Backend Services

Sensitive information such as JWTs and authentication requests must not be transmitted over unencrypted production HTTP.

28. Secrets Management

The following must be treated as secrets:

JWT signing secret
Database passwords
Payment provider credentials
OTP provider credentials
External API keys
Deployment credentials

Secrets must:

Remain outside source control.
Be supplied through environment/deployment configuration.
Not be printed in logs.
Not be embedded in frontend source code.
29. Frontend Security

The React frontend is not a trusted security boundary.

The frontend may:

Display authenticated state.
Store/use the access token according to the selected client strategy.
Hide unauthorized UI options.

But backend services must independently enforce:

Authentication
Authorization
Ownership
Validation
Business rules

Hiding a button is not an authorization mechanism.

30. JWT Tampering

The backend must validate:

Token signature
Expiration
Required claims
Expected token structure
Appropriate issuer/audience where configured

A client must not be able to modify:

role = ADMIN

or another authorization claim and gain privileges.

31. Booking Security

Booking operations must combine security with concurrency controls.

A valid JWT does not automatically mean that a booking should succeed.

The Booking Service must still validate:

Authenticated user
+
Valid show
+
Valid seats
+
Seat availability
+
Hold ownership
+
Booking state
+
Business rules
32. Hold Ownership

A customer must only be able to continue or complete a hold that belongs to the authenticated booking/user context.

Example:

User A
 ↓
Hold A → allowed

User B
 ↓
Hold A → rejected

The server must validate ownership.

A client must not be able to modify another user's hold by changing a hold ID.

33. Payment Callback Security

Payment provider callbacks/webhooks must not be trusted solely because they reach a public endpoint.

The Payment Service should validate provider-specific authenticity mechanisms where supported.

Examples may include:

Signature verification
Webhook secret
Provider transaction verification

The exact mechanism depends on the payment provider used.

For Review 1, a simulated payment provider may implement a simplified trusted callback model.

34. Idempotency and Security

Idempotency keys must be scoped appropriately.

A malicious client should not be able to use another user's idempotency operation to retrieve or manipulate another user's result.

The system should associate critical idempotency records with the relevant:

User
Operation
Resource
Idempotency key

where appropriate.

35. Error Message Security

Error responses should provide enough information for the client to understand the failure without exposing internal implementation details.

Avoid returning:

Database stack trace
SQL query
Internal credentials
JWT secret
Internal filesystem path

to clients.

Detailed technical information belongs in controlled server-side logs.

36. Logging Security

Logs should never contain:

Passwords
OTP values
JWT secrets
Database passwords
Payment credentials
Card information
Provider secrets

Logs may contain safe identifiers such as:

request_id
booking_id
payment_id
user_id

when required for troubleshooting and permitted by the application's privacy requirements.

37. Role Separation

Roles must remain separate:

CUSTOMER
THEATRE_OPERATOR
ADMIN

A customer must not gain operator permissions through a request payload.

Role assignment must be controlled by the User Service and administrative workflows.

38. Service-Level Security

Each service should expose only the APIs required for its responsibility.

Examples:

Movie Service
→ movie operations

Theatre Service
→ theatre/screen/physical-seat operations

Show Service
→ show/pricing/policy operations

Booking Service
→ inventory/hold/booking/ticket operations

Payment Service
→ payment/refund operations

Internal implementation endpoints should not be unnecessarily exposed publicly.

39. API Gateway Routing Security

The Gateway should route requests only to known service routes.

Example:

/api/movies/**     → Movie Service
/api/theatres/**   → Theatre Service
/api/shows/**      → Show Service
/api/bookings/**   → Booking Service
/api/payments/**   → Payment Service
/api/users/**      → User Service

Exact route patterns will be finalized in the API contract.

40. Service Discovery Security

Eureka is an infrastructure component.

It should not be treated as a public business API.

Only authorized services/infrastructure components should communicate with the service registry in the deployed environment.

41. Rate Limiting

Rate limiting should be considered for sensitive or abuse-prone operations.

Priority areas include:

OTP generation
OTP verification
Authentication attempts
Booking creation
Payment initiation

Rate limiting protects services from accidental or malicious request bursts.

The exact limits should be determined during implementation and testing.

42. High-Concurrency Security Interaction

Security controls must not introduce unsafe concurrency behavior.

For example, authentication validation must happen before protected booking operations, but the seat-allocation transaction remains responsible for concurrency correctness.

The architecture therefore separates:

Authentication/Authorization
        ↓
Business Validation
        ↓
Concurrency-Controlled Allocation

A valid JWT does not reserve a seat.

Only the Booking Service's authoritative inventory operation can do that.

43. Security Failure Behavior

Security failures must fail closed.

Examples:

Invalid JWT
    ↓
401 Unauthorized

Valid JWT but insufficient role
    ↓
403 Forbidden

Invalid OTP
    ↓
Authentication rejected

Customer accessing another customer's booking
    ↓
Authorization rejected

The exact HTTP response mapping will be finalized in the API contract.

44. Review 1 Security Demonstrations

The following should be demonstrated during testing:

Test 1 — Valid JWT
Valid JWT
 ↓
Protected booking API
 ↓
Request accepted if business conditions are satisfied
Test 2 — Missing JWT
No token
 ↓
Protected API
 ↓
Rejected
Test 3 — Expired JWT
Expired token
 ↓
Protected API
 ↓
Rejected
Test 4 — Role Restriction
CUSTOMER
 ↓
Theatre operator API
 ↓
Rejected
Test 5 — Booking Ownership
Customer A
 ↓
Customer B's booking
 ↓
Rejected
Test 6 — OTP Expiration
Expired OTP
 ↓
Verification
 ↓
Rejected
Test 7 — Duplicate Payment Request
Same payment idempotency key
 ↓
Repeated request
 ↓
No duplicate payment effect
45. Security Architecture Summary

The security model is:

                 ┌──────────────────┐
                 │ React Frontend   │
                 └────────┬─────────┘
                          │
                       HTTPS
                          │
                          ▼
                 ┌──────────────────┐
                 │  API Gateway     │
                 └────────┬─────────┘
                          │
                   JWT/Auth Context
                          │
          ┌───────────────┼────────────────┐
          ▼               ▼                ▼
     User Service     Business         Payment
                      Services          Service
          │               │                │
          ▼               ▼                ▼
       User DB        Service DB       Payment DB

The core security responsibilities are:

User Service
→ Authentication
→ OTP
→ User identity
→ Roles

API Gateway
→ Public entry point
→ Routing
→ Initial security filtering

Business Services
→ Authorization
→ Ownership checks
→ Business validation

Booking Service
→ Secure seat allocation
→ Hold ownership
→ Booking ownership

Payment Service
→ Payment authorization
→ Idempotency
→ Payment state/reconciliation
46. Frozen Security Decisions
Area	Decision
Authentication	JWT
Initial login mechanism	Phone + OTP
OTP validity	5 minutes
Roles	CUSTOMER, THEATRE_OPERATOR, ADMIN
User identity owner	User Service
JWT secret	Environment/deployment configuration
Protected APIs	Backend-enforced
Authorization	Role + resource ownership
Passwords	Not primary customer authentication
Database access	Service-owned only
Frontend trust	Untrusted
Production transport	HTTPS
Sensitive secrets	Never committed
Payment security	Payment Service boundary
Payment callback	Authenticated/verified mechanism
Critical operations	Idempotency required
Error exposure	No internal implementation details
OTP abuse protection	Rate limiting + attempt limits
47. Implementation Constraint

AI agents implementing BookNGo must preserve these security boundaries.

Agents must not:

Hardcode JWT secrets.
Hardcode database passwords.
Store OTPs or credentials in frontend code.
Trust role information directly from request bodies.
Allow customers to access other customers' bookings.
Allow operators to manage unassigned theatres.
Expose service databases directly.
Treat frontend authorization as sufficient.
Disable authentication on protected APIs for convenience.
Log JWT secrets, OTPs, passwords, or payment credentials.
Bypass Booking Service authorization to manipulate seat inventory.
Introduce cross-service database access.

Any change to the security architecture must be explicitly reviewed before implementation.

