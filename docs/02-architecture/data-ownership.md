# BookNGo — Data Ownership Architecture

## 1. Purpose

This document defines the data ownership boundaries for the BookNGo microservices architecture.

It establishes:

- Which service owns each business entity.
- Which database stores each entity.
- Which service is the authoritative source of truth.
- Cross-service references.
- Data that must not be shared directly.
- The distinction between physical cinema infrastructure and show-specific booking inventory.
- Data required for concurrency control.
- Data required for idempotency and recovery.

This document is a logical ownership definition.

The detailed database schema, columns, constraints, indexes, and relationships will be designed later in:

```text
docs/04-database/ERD.md
docs/04-database/schema.sql
2. Data Ownership Principles
2.1 Database Per Business Service

Each business microservice owns its own persistent data.

The six business services therefore have six logical databases:

User & Identity Service
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
2.2 Single Source of Truth

Each business entity must have one authoritative owner.

For example:

Movie
    -> Movie Service

Physical Seat
    -> Theatre Service

Show
    -> Show Service

Show-Seat Availability
    -> Booking Service

Booking
    -> Booking Service

Payment
    -> Payment Service

Other services may store identifiers or limited read-oriented information, but they must not become a competing authoritative source.

2.3 No Shared Database

The services must not use a shared business database.

This is prohibited:

Movie Service -----+
                   |
Show Service ------+----> Shared Database
                   |
Booking Service ---+

Instead:

Movie Service
      |
      v
Movie Database

Show Service
      |
      v
Show Database

Booking Service
      |
      v
Booking Database
3. Database Ownership Overview
Service	Database	Primary Data
User & Identity	User Database	Users, credentials, OTP, roles
Movie	Movie Database	Movies, movie metadata
Theatre	Theatre Database	Theatres, screens, physical seats, operator assignments
Show	Show Database	Shows, pricing, cancellation policies
Booking	Booking Database	Show-seat inventory, holds, bookings, tickets
Payment	Payment Database	Payment attempts, payment state, refunds/reversals
4. User & Identity Database
4.1 Owner

User & Identity Service

4.2 Authoritative Data

The database owns identity and authentication information.

Logical entities include:

User
Role
UserRole
OTP / OTP Verification
Authentication-related records

The exact table structure will be finalized during database design.

4.3 User Data

A user record may contain information such as:

User ID.
Name.
Phone number.
Email where applicable.
Account status.
Creation timestamp.
Update timestamp.

Authentication credentials and sensitive authentication data must be handled securely.

4.4 Role Data

The service owns application roles such as:

CUSTOMER
THEATRE_OPERATOR
ADMIN

The relationship between users and roles is owned by User & Identity Service.

4.5 OTP Data

OTP-related state belongs to User & Identity Service.

Logical information may include:

OTP identifier.
User/phone reference.
OTP verification state.
Creation time.
Expiration time.
Attempt count.
Rate-limiting information.

The OTP lifetime is governed by the requirements.

4.6 Does Not Own

User & Identity Database must not store:

Movie records.
Theatre records.
Physical seats.
Shows.
Seat inventory.
Bookings.
Payments.
Tickets.
5. Movie Database
5.1 Owner

Movie Service

5.2 Authoritative Data

The Movie Database owns movie catalogue information.

Logical entity:

Movie
5.3 Movie Data

A movie may contain:

Movie ID.
Title.
Description.
Language.
Genre.
Duration.
Release information.
Poster/image reference.
Status.
Additional metadata required by the application.

The exact attributes will be finalized during schema design.

5.4 Does Not Own

Movie Database must not store authoritative:

Theatre records.
Screen records.
Physical seats.
Show schedules.
Show-seat inventory.
Bookings.
Payments.
6. Theatre Database
6.1 Owner

Theatre Service

6.2 Authoritative Data

The Theatre Database owns the physical cinema infrastructure.

Logical entities include:

Theatre
Screen
Physical Seat
Theatre Operator Assignment
7. Theatre Entity

A Theatre represents a physical cinema location.

Logical information may include:

Theatre ID.
Theatre name.
Address.
City.
Location information.
Status.
Operator assignment reference.
Creation/update timestamps.

Theatre Service is authoritative for this information.

8. Screen Entity

A Screen belongs to a Theatre.

Conceptually:

Theatre
   |
   +---- Screen 1
   |
   +---- Screen 2
   |
   +---- Screen 3

A screen may contain:

Screen ID.
Theatre ID.
Screen name/number.
Screen type/format where applicable.
Status.
Configuration information.
9. Physical Seat Entity

Physical seats belong to screens.

Conceptually:

Theatre
   |
   +-- Screen 1
          |
          +-- A1
          +-- A2
          +-- A3
          +-- A4

A physical seat may contain:

Physical Seat ID.
Screen ID.
Row.
Seat number/label.
Category such as Regular/Premium/VIP where appropriate.
Status.

Theatre Service is authoritative for the existence and configuration of physical seats.

10. Physical Seat vs Show-Seat Inventory

This is one of the most important data ownership decisions in BookNGo.

A physical seat represents:

A real seat that physically exists inside a cinema screen.

A show-seat inventory record represents:

The booking state of that physical seat for one particular show.

They are different concepts.

Example:

Physical Seat:
    Screen 1 -> A10

The same physical seat may be used for:

Show 101 -> A10
Show 102 -> A10
Show 103 -> A10

Each show has an independent booking state.

Therefore:

Physical Seat
    -> Theatre Database

Show-Seat Inventory
    -> Booking Database
11. Show Database
11.1 Owner

Show Service

11.2 Authoritative Data

The Show Database owns:

Show
Pricing
Cancellation Policy
12. Show Entity

A show represents a scheduled screening.

A logical show record may contain:

Show ID.
Movie ID.
Theatre ID.
Screen ID.
Start time.
End time.
Show status.
Format information where applicable.
Creation/update timestamps.

The referenced movie, theatre, and screen remain owned by their respective services.

13. Cross-Service References in Show

The Show Service may store:

movie_id
theatre_id
screen_id

These are references.

They do not transfer ownership.

Ownership remains:

movie_id
    -> Movie Service

theatre_id
    -> Theatre Service

screen_id
    -> Theatre Service

The Show Database must not attempt to become the authoritative store for those entities.

14. Pricing Data

Show-specific pricing belongs to Show Service.

Logical pricing information may include:

Show reference.
Seat category.
Price.
Currency.
Effective status.

Example:

Show 101

Regular  -> ₹180
Premium  -> ₹220
VIP      -> ₹280

The exact pricing model will be finalized in database design.

Payment Service does not define the business price.

It processes the amount supplied by the booking/payment workflow.

15. Cancellation Policy Data

Show Service owns cancellation policy configuration.

Logical information may include:

Show reference.
Cancellable/non-cancellable status.
Cancellation deadline.
Policy status.

Example:

Show 101
    Cancellable: YES
    Deadline: 12 hours before show

Booking Service uses this policy when executing cancellation.

16. Booking Database
16.1 Owner

Booking Service

16.2 Authoritative Data

The Booking Database owns the booking domain.

Logical entities include:

Show-Seat Inventory
Seat Hold
Booking
Booking Seat
Ticket
Idempotency Record

Additional supporting records may be introduced if required by the final design.

17. Show-Seat Inventory

The Show-Seat Inventory is the central data structure for high-concurrency seat allocation.

It represents the state of a physical seat for a specific show.

Conceptually:

Show 101 + Physical Seat A10
                |
                v
        Show-Seat Inventory
                |
                v
             HELD

Another show can independently have:

Show 102 + Physical Seat A10
                |
                v
        Show-Seat Inventory
                |
                v
           AVAILABLE

Therefore the logical uniqueness boundary is:

(show_id, physical_seat_id)
18. Show-Seat Inventory State

The primary states are:

AVAILABLE
HELD
BOOKED

Additional internal states may be introduced only if required by the final implementation.

The inventory state is authoritative in Booking Service.

19. Seat Hold Data

Booking Service owns temporary seat holds.

Logical information includes:

Hold ID.
Show ID.
Customer/user ID.
Selected physical seat IDs.
Hold state.
Created timestamp.
Expiration timestamp.
Associated booking reference where applicable.

The normal hold duration is:

5 minutes

The server is authoritative for expiration.

20. Booking Data

Booking Service owns booking records.

A logical booking may contain:

Booking ID.
Customer/user ID.
Show ID.
Amount.
Booking state.
Creation timestamp.
Update timestamp.
Idempotency information.
Relevant payment reference.

The selected seats are represented through booking-seat information.

21. Booking State

The booking lifecycle is conceptually:

INITIATED
    |
    v
HELD
    |
    v
PAYMENT_PENDING
    |
    +----> PAYMENT_FAILED
    |
    +----> EXPIRED
    |
    v
CONFIRMED
    |
    v
TICKET_ISSUED

Cancellation may transition a confirmed booking into:

CANCELLED

The exact persistence model will be finalized during database design.

22. Booking Seat Data

A booking may contain multiple selected seats.

Example:

Booking B1001

Show: 101

Seats:
    A10
    A11
    A12

The relationship between a booking and its selected seats belongs to Booking Service.

The physical seat itself remains owned by Theatre Service.

23. Ticket Data

Ticket information belongs to Booking Service.

A ticket is generated after successful booking confirmation.

Logical information may include:

Ticket ID.
Booking ID.
Show reference.
Customer reference.
Seat information/reference.
Ticket status.
Issuance timestamp.
Booking/ticket code.

A separate Ticket Service is not introduced.

24. Idempotency Data

Booking Service must support idempotent operations.

Logical idempotency information may include:

Idempotency key.
Operation type.
Request owner/user.
Associated resource.
Request/result status.
Creation timestamp.

This supports safe retry behavior.

Example:

Client
   |
   | Create booking
   | Idempotency-Key: ABC123
   v
Booking Service
   |
   v
Booking B1001

If the same logical request is retried:

Idempotency-Key: ABC123

the system should not create a second booking.

25. Payment Database
25.1 Owner

Payment Service

25.2 Authoritative Data

The Payment Database owns:

Payment
Payment Attempt
Payment State
Refund / Reversal
Payment Reconciliation Information
Idempotency Information
26. Payment Data

A payment record may contain:

Payment ID.
Booking ID reference.
Amount.
Currency.
Payment state.
Provider/reference information.
Idempotency key.
Creation timestamp.
Update timestamp.

The payment record references the booking but does not own the booking.

27. Payment State

Supported payment outcomes include:

INITIATED
SUCCESS
FAILURE
CANCELLED
TIMEOUT
UNKNOWN

The Payment Service is authoritative for these states.

28. Refund and Reversal Data

Payment Service owns refund/reversal state.

Conceptually:

Payment SUCCESS
      |
      v
Booking confirmation failure
      |
      v
Refund / Reversal
      |
      v
REFUND_PENDING
      |
      v
REFUNDED

The exact provider-specific behavior is hidden behind the Payment Service boundary.

29. Payment Idempotency

Payment operations must be idempotent.

Logical information may include:

Idempotency Key
Payment ID
Booking ID
Operation
Request Status
Result

A repeated payment request with the same logical idempotency key must not create an unintended duplicate payment.

30. Cross-Service Identifier Strategy

Services should reference entities using stable identifiers.

Examples:

user_id
movie_id
theatre_id
screen_id
physical_seat_id
show_id
hold_id
booking_id
ticket_id
payment_id

The identifier does not imply ownership.

For example:

Booking Database
    |
    +-- show_id
    +-- user_id
    +-- physical_seat_id

does not mean Booking Service owns:

Show
User
Physical Seat

Those remain owned by their respective services.

31. Cross-Service Foreign Key Rule

Cross-service identifiers must not be implemented as direct relational foreign keys across databases.

For example:

Booking Database
      |
      X----> Show Database foreign key

is not permitted.

Instead:

Booking Database
      |
      +-- show_id

The validity of the referenced entity is established through service-level communication and business rules.

Within a service's own database, normal relational foreign keys may be used.

32. Data Duplication Rule

Limited duplication of data may be permitted when necessary for:

Read performance.
Resilience.
Historical snapshots.
Reporting.

However, duplicated information must have a clearly identified source of truth.

For example, a booking may store a snapshot of information required for ticket/history display.

That snapshot does not transfer ownership of the original entity.

33. Historical Booking Information

Bookings and tickets may need to preserve historical information even when source data changes later.

For example, a ticket may need to retain enough information to identify:

Movie.
Theatre.
Screen.
Show.
Seats.
Amount.

The exact snapshot strategy will be determined during database design.

The principle is:

Historical booking information must remain understandable without violating service ownership.

34. Data Ownership During Booking

A simplified ownership view of the booking process is:

User
 |
 +-- User & Identity Service

Movie
 |
 +-- Movie Service

Theatre
 |
 +-- Theatre Service
       |
       +-- Screen
             |
             +-- Physical Seat

Show
 |
 +-- Show Service
       |
       +-- Pricing
       +-- Cancellation Policy

Show + Physical Seat
 |
 +-- Booking Service
       |
       +-- Availability
       +-- Hold
       +-- Booking
       +-- Ticket

Booking + Payment
 |
 +-- Payment Service
       |
       +-- Payment
       +-- Refund/Reversal
35. Concurrency-Critical Data

The most concurrency-sensitive data belongs to Booking Database.

The critical logical record is:

(show_id, physical_seat_id)

The database and application design must ensure that concurrent requests cannot successfully allocate the same combination more than once.

The detailed mechanism will be defined in:

docs/02-architecture/concurrency-strategy.md
36. Data Required for Hold Expiration

Booking Service must persist enough information to recover expired holds.

At minimum, hold data must allow the system to determine:

When was the hold created?
When does the hold expire?
Which show does it belong to?
Which seats does it contain?
Which booking/customer does it belong to?
What is the current hold state?

The system must not depend solely on an in-memory timer.

This allows hold expiration to survive:

Service restart.
Process failure.
Client disconnection.
37. Data Required for Payment Recovery

Payment Service must persist enough information to determine the outcome of uncertain payment operations.

For UNKNOWN or TIMEOUT situations, the system must retain:

Payment identifier.
Booking reference.
Amount.
Provider/reference information where applicable.
Idempotency key.
Current payment state.
Timestamps.

This allows reconciliation without blindly creating another payment.

38. Data Required for Booking Recovery

Booking Service must persist enough information to recover booking state after service restart.

Persistent state must include enough information to determine:

Booking status.
Selected seats.
Show.
Customer.
Hold expiration.
Payment reference.
Confirmation state.

The service must not rely exclusively on process memory for authoritative booking state.

39. Data Ownership During Failure

Each service remains authoritative for its own state during failures.

Example:

Payment Service unavailable
        |
        v
Payment state cannot be confirmed
        |
        v
Booking must not assume payment SUCCESS

Another example:

Booking Service restarts
        |
        v
Persistent Booking Database
        |
        v
Recover booking/hold state

The detailed failure behavior is defined separately.

40. Data Access Rules

The following rules apply:

Rule 1

A service may read and modify its own database.

Rule 2

A service must not directly access another service's database.

Rule 3

Cross-service data is accessed through APIs or approved messaging mechanisms.

Rule 4

Cross-service identifiers are references, not shared relational ownership.

Rule 5

Each business concept has one authoritative owner.

Rule 6

Booking Service owns show-specific seat state.

Rule 7

Theatre Service owns physical seat definitions.

Rule 8

Show Service owns pricing and cancellation policy.

Rule 9

Payment Service owns payment and refund state.

Rule 10

Persistent state must be sufficient for recovery.

Rule 11

Idempotency information must be persisted where required for safe retries.

41. Final Data Ownership Matrix
Entity / Data	Owner Service	Database
User	User & Identity	User DB
Role	User & Identity	User DB
OTP	User & Identity	User DB
Movie	Movie	Movie DB
Theatre	Theatre	Theatre DB
Screen	Theatre	Theatre DB
Physical Seat	Theatre	Theatre DB
Theatre Operator Assignment	Theatre	Theatre DB
Show	Show	Show DB
Pricing	Show	Show DB
Cancellation Policy	Show	Show DB
Show-Seat Inventory	Booking	Booking DB
Seat Hold	Booking	Booking DB
Booking	Booking	Booking DB
Booking Seat	Booking	Booking DB
Ticket	Booking	Booking DB
Booking Idempotency	Booking	Booking DB
Payment	Payment	Payment DB
Payment Attempt	Payment	Payment DB
Payment Reconciliation	Payment	Payment DB
Refund/Reversal	Payment	Payment DB
Payment Idempotency	Payment	Payment DB
42. Final Data Architecture

The final logical database architecture is:

                         +----------------------+
                         | User & Identity      |
                         | Service              |
                         +----------+-----------+
                                    |
                                    v
                              +-----------+
                              | User DB   |
                              +-----------+


                         +----------------------+
                         | Movie Service        |
                         +----------+-----------+
                                    |
                                    v
                              +-----------+
                              | Movie DB  |
                              +-----------+


                         +----------------------+
                         | Theatre Service      |
                         +----------+-----------+
                                    |
                                    v
                              +-----------+
                              | Theatre DB|
                              +-----------+
                                    |
                         +----------+----------+
                         |                     |
                     Theatre               Physical
                                             Seats


                         +----------------------+
                         | Show Service         |
                         +----------+-----------+
                                    |
                                    v
                              +-----------+
                              | Show DB   |
                              +-----------+


                         +----------------------+
                         | Booking Service      |
                         +----------+-----------+
                                    |
                                    v
                              +-----------+
                              | Booking DB|
                              +-----------+
                                    |
                     +--------------+--------------+
                     |              |              |
                 Show-Seat        Holds         Bookings
                 Inventory                       Tickets


                         +----------------------+
                         | Payment Service      |
                         +----------+-----------+
                                    |
                                    v
                              +-----------+
                              | Payment DB|
                              +-----------+
                                    |
                            +-------+-------+
                            |               |
                         Payments       Refunds
43. Most Important Ownership Boundary

The following distinction must remain unchanged throughout implementation:

THEATRE SERVICE
    |
    +-- Theatre
    +-- Screen
    +-- Physical Seat


SHOW SERVICE
    |
    +-- Show
    +-- Pricing
    +-- Cancellation Policy


BOOKING SERVICE
    |
    +-- Show-Seat Inventory
    +-- Seat Hold
    +-- Booking
    +-- Ticket

In particular:

Physical Seat != Show-Seat Inventory

A physical seat is permanent infrastructure.

Show-seat inventory is a temporary/transactional representation of that seat's state for a particular show.

This distinction is fundamental to the high-concurrency booking architecture.

44. Data Architecture Baseline

The approved data ownership architecture is:

User & Identity -> User DB
Movie           -> Movie DB
Theatre         -> Theatre DB
Show            -> Show DB
Booking         -> Booking DB
Payment         -> Payment DB

with the following critical boundaries:

Physical Seat
    -> Theatre Service

Show-Seat Inventory
    -> Booking Service

Seat Hold
    -> Booking Service

Booking
    -> Booking Service

Ticket
    -> Booking Service

Payment
    -> Payment Service

Refund/Reversal
    -> Payment Service

No database schema or implementation should violate these ownership boundaries without an explicit architecture decision.
