# BookNGo — Database Design & ERD

## 1. Purpose

This document defines the logical database design for BookNGo. The system uses a database-per-service approach so each microservice owns and controls its persistent data.

The design focuses on:
- clear data ownership
- correct seat allocation under concurrency
- bounded seat holds
- safe booking/payment recovery
- idempotent operations
- minimal cross-service coupling

---

## 2. Database Strategy

BookNGo contains six business services. Each service owns its own PostgreSQL database.

| Service | Database | Primary Responsibility |
|---|---|---|
| User Service | `user_db` | `users`, `roles`, `user_roles`, `otp_verifications` |
| Movie Service | `movie_db` | `movies` |
| Theatre Service | `theatre_db` | `theatres`, `screens`, `physical_seats`, `theatre_operator_assignments` |
| Show Service | `show_db` | `shows`, `show_pricing`, `cancellation_policies` |
| Booking Service | `booking_db` | `show_seat_inventory`, `seat_holds`, `bookings`, `booking_seats`, `tickets`, `booking_idempotency_records` |
| Payment Service | `payment_db` | `payments`, `payment_attempts`, `refund_reversals`, `payment_reconciliations`, `payment_idempotency_records` |

Eureka Server and API Gateway do not require business-domain databases.

### Cross-Service Data Rule

Foreign keys are used only between tables owned by the same service.
References to another service use application-level IDs (UUIDs). A service must not directly join another service's database.

---

## 3. Global Conventions

- PostgreSQL is the database engine.
- Primary keys use UUIDs (named `<entity>_id` matching `schema.sql`).
- Timestamps use `TIMESTAMPTZ`.
- Monetary values use `NUMERIC(12,2)`.
- Status fields use controlled values/enums at the application level.
- Every persistent entity has `created_at`.
- Mutable entities also have `updated_at`.
- Cross-service IDs are stored as UUID references without database-level foreign keys.

---

## 4. User Service Database (`user_db`)

### 4.1 `users`

Stores registered platform users.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `user_id` | UUID | PK | NO | User identifier |
| `full_name` | VARCHAR(150) | | NO | Display name |
| `phone_number` | VARCHAR(32) | UNIQUE | NO | Contact/login phone number |
| `email` | VARCHAR(320) | | YES | Contact email |
| `password_hash` | VARCHAR(255) | | YES | Hashed password (NULL for CUSTOMER using phone+OTP; required for THEATRE_OPERATOR & ADMIN) |
| `account_status` | VARCHAR(32) | | NO | Account status (`ACTIVE`, `INACTIVE`, `SUSPENDED`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

### 4.2 `roles`

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `role_code` | VARCHAR(32) | PK | NO | Role identifier (`CUSTOMER`, `THEATRE_OPERATOR`, `ADMIN`) |

### 4.3 `user_roles`

Associates users with roles.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `user_id` | UUID | PK/FK | NO | References `users.user_id` |
| `role_code` | VARCHAR(32) | PK/FK | NO | References `roles.role_code` |
| `created_at` | TIMESTAMPTZ | | NO | Assignment time |

### 4.4 `otp_verifications`

Supports verification flows when OTP authentication is used.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `otp_id` | UUID | PK | NO | Verification identifier |
| `user_id` | UUID | FK | YES | References `users.user_id` |
| `phone_number` | VARCHAR(32) | | NO | Target phone number |
| `otp_secret_hash` | VARCHAR(255) | | NO | Hashed OTP secret |
| `status` | VARCHAR(32) | | NO | Status (`PENDING`, `VERIFIED`, `EXPIRED`, `REJECTED`, `LOCKED`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `expires_at` | TIMESTAMPTZ | | NO | Expiry time |
| `verified_at` | TIMESTAMPTZ | | YES | Successful verification time |
| `attempt_count` | INTEGER | | NO | Attempt counter |
| `rate_limit_metadata` | JSONB | | NO | Rate limiting metadata |

---

## 5. Movie Database (`movie_db`)

### 5.1 `movies`

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `movie_id` | UUID | PK | NO | Movie identifier |
| `title` | VARCHAR(255) | | NO | Movie title |
| `description` | TEXT | | YES | Synopsis |
| `language` | VARCHAR(64) | | NO | Primary language |
| `genres` | JSONB | | NO | JSON array of genres |
| `duration_minutes` | INTEGER | | NO | Runtime in minutes (> 0) |
| `release_date` | DATE | | YES | Release date |
| `poster_reference` | TEXT | | YES | Poster image URL/path |
| `metadata` | JSONB | | NO | Extended movie metadata |
| `status` | VARCHAR(32) | | NO | Status (`DRAFT`, `ACTIVE`, `INACTIVE`, `ARCHIVED`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

---

## 6. Theatre Database (`theatre_db`)

### 6.1 `theatres`

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `theatre_id` | UUID | PK | NO | Theatre identifier |
| `name` | VARCHAR(255) | | NO | Theatre name |
| `address` | TEXT | | NO | Full address |
| `city` | VARCHAR(128) | | NO | City |
| `location_metadata` | JSONB | | NO | Location metadata |
| `status` | VARCHAR(32) | | NO | Status (`ACTIVE`, `INACTIVE`, `CLOSED`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

### 6.2 `screens`

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `screen_id` | UUID | PK | NO | Screen identifier |
| `theatre_id` | UUID | FK | NO | References `theatres.theatre_id` |
| `name` | VARCHAR(100) | | NO | Screen name/number |
| `screen_format` | VARCHAR(64) | | YES | Format (e.g. IMAX, 2D, 3D) |
| `configuration` | JSONB | | NO | Screen layout configuration |
| `status` | VARCHAR(32) | | NO | Status (`ACTIVE`, `INACTIVE`, `MAINTENANCE`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraint: `UNIQUE(theatre_id, name)`

### 6.3 `physical_seats`

Permanent physical seats belonging to a screen.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `physical_seat_id` | UUID | PK | NO | Physical seat identifier |
| `screen_id` | UUID | FK | NO | References `screens.screen_id` |
| `row_label` | VARCHAR(20) | | NO | Row identifier (e.g. 'A') |
| `seat_number` | VARCHAR(20) | | NO | Seat number (e.g. '12') |
| `seat_category` | VARCHAR(32) | | NO | Category representation (`REGULAR`, `PREMIUM`, `VIP`) |
| `status` | VARCHAR(32) | | NO | Status (`ACTIVE`, `INACTIVE`, `MAINTENANCE`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraint: `UNIQUE(screen_id, row_label, seat_number)`

### 6.4 `theatre_operator_assignments`

Maps operators to theatres.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `assignment_id` | UUID | PK | NO | Assignment identifier |
| `theatre_id` | UUID | FK | NO | References `theatres.theatre_id` |
| `user_id` | UUID | | NO | Application reference to User Service |
| `status` | VARCHAR(32) | | NO | Status (`ACTIVE`, `INACTIVE`) |
| `created_at` | TIMESTAMPTZ | | NO | Assignment time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraint: `UNIQUE(theatre_id, user_id)`

---

## 7. Show Database (`show_db`)

### 7.1 `shows`

A scheduled movie screening.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `show_id` | UUID | PK | NO | Show identifier |
| `movie_id` | UUID | | NO | Application reference to Movie Service |
| `theatre_id` | UUID | | NO | Application reference to Theatre Service |
| `screen_id` | UUID | | NO | Application reference to Theatre Service |
| `starts_at` | TIMESTAMPTZ | | NO | Show start time |
| `ends_at` | TIMESTAMPTZ | | NO | Show end time |
| `show_format` | VARCHAR(64) | | YES | Format override/info |
| `status` | VARCHAR(32) | | NO | Status (`DRAFT`, `SCHEDULED`, `CANCELLED`, `COMPLETED`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraint: `CHECK (ends_at > starts_at)`

### 7.2 `show_pricing`

Stores price by seat category for a show.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `show_pricing_id` | UUID | PK | NO | Pricing identifier |
| `show_id` | UUID | FK | NO | References `shows.show_id` |
| `seat_category` | VARCHAR(32) | | NO | Category representation (`REGULAR`, `PREMIUM`, `VIP`) |
| `amount` | NUMERIC(12,2) | | NO | Price amount (>= 0) |
| `currency` | CHAR(3) | | NO | Currency code |
| `status` | VARCHAR(32) | | NO | Status (`ACTIVE`, `INACTIVE`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Unique Index: `UNIQUE(show_id, seat_category) WHERE status = 'ACTIVE'`

### 7.3 `cancellation_policies`

Defines cancellation rules for a show.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `cancellation_policy_id` | UUID | PK | NO | Policy identifier |
| `show_id` | UUID | FK | NO | References `shows.show_id` |
| `policy_type` | VARCHAR(32) | | NO | Type (`CANCELLABLE`, `NON_CANCELLABLE`) |
| `cancellation_deadline` | TIMESTAMPTZ | | YES | Effective deadline for cancellation |
| `status` | VARCHAR(32) | | NO | Status (`ACTIVE`, `INACTIVE`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraint: `UNIQUE(show_id)`

---

## 8. Booking Database (`booking_db`)

This database owns the authoritative booking-side seat allocation state.

### 8.1 `bookings`

Represents the user's booking/order.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `booking_id` | UUID | PK | NO | Booking identifier |
| `user_id` | UUID | | NO | Application reference to User Service |
| `show_id` | UUID | | NO | Application reference to Show Service |
| `total_amount` | NUMERIC(12,2) | | NO | Total booking amount |
| `currency` | CHAR(3) | | NO | Currency code |
| `status` | VARCHAR(32) | | NO | Booking status (12-state model) |
| `payment_grace_expires_at` | TIMESTAMPTZ | | YES | Optional payment grace expiry |
| `payment_id` | UUID | | YES | Application reference to Payment Service |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraint: `UNIQUE(booking_id, user_id, show_id)`

#### Booking Status 12-State Model
`INITIATED`, `HELD`, `PAYMENT_PENDING`, `CONFIRMED`, `TICKET_ISSUED`, `EXPIRED`, `CANCELLED`, `PAYMENT_FAILED`, `PAYMENT_CANCELLED`, `PAYMENT_UNKNOWN`, `REFUND_PENDING`, `REFUNDED`

### 8.2 `seat_holds`

Represents a temporary reservation tied to an initiated booking.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `hold_id` | UUID | PK | NO | Hold identifier |
| `booking_id` | UUID | FK | NO | References `bookings.booking_id` (UNIQUE) |
| `user_id` | UUID | | NO | Application reference to User Service |
| `show_id` | UUID | | NO | Application reference to Show Service |
| `status` | VARCHAR(32) | | NO | Status (`ACTIVE`, `EXPIRED`, `RELEASED`, `CONVERTED`) |
| `created_at` | TIMESTAMPTZ | | NO | Hold creation time |
| `normal_expires_at` | TIMESTAMPTZ | | NO | Normal 5-minute expiry |
| `payment_grace_expires_at` | TIMESTAMPTZ | | YES | Maximum 2-minute payment grace |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraints:
- `booking_id`: `NOT NULL UNIQUE REFERENCES bookings(booking_id)`
- `FOREIGN KEY (booking_id, user_id, show_id) REFERENCES bookings(booking_id, user_id, show_id)`
- `UNIQUE(hold_id, show_id)`

### 8.3 `show_seat_inventory`

One row represents one physical seat for one show.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `show_seat_inventory_id` | UUID | PK | NO | Inventory row identifier |
| `show_id` | UUID | | NO | Application reference to Show Service |
| `physical_seat_id` | UUID | | NO | Application reference to Theatre Service |
| `status` | VARCHAR(16) | | NO | Status (`AVAILABLE`, `HELD`, `BOOKED`) |
| `active_hold_id` | UUID | FK | YES | FK to `seat_holds(hold_id, show_id)` |
| `current_booking_id` | UUID | FK | YES | FK to `bookings(booking_id)` |
| `hold_expires_at` | TIMESTAMPTZ | | YES | Effective hold expiry |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Critical constraint: `UNIQUE(show_id, physical_seat_id)`

### 8.4 `booking_seats`

Stores the seats belonging to a booking and preserves booking-time snapshots.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `booking_seat_id` | UUID | PK | NO | Booking-seat identifier |
| `booking_id` | UUID | FK | NO | References `bookings.booking_id` |
| `show_seat_inventory_id` | UUID | FK | NO | References `show_seat_inventory.show_seat_inventory_id` |
| `physical_seat_id` | UUID | | NO | Application reference to Theatre Service |
| `seat_label_snapshot` | VARCHAR(64) | | NO | Seat display label snapshot |
| `seat_category_snapshot` | VARCHAR(32) | | NO | Category snapshot |
| `unit_price_snapshot` | NUMERIC(12,2) | | NO | Unit price snapshot |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |

Constraint: `UNIQUE(booking_id, physical_seat_id)`

### 8.5 `tickets`

One ticket is issued for each confirmed booking.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `ticket_id` | UUID | PK | NO | Ticket identifier |
| `booking_id` | UUID | FK | NO | References `bookings.booking_id` (UNIQUE) |
| `ticket_code` | VARCHAR(100) | UNIQUE | NO | Ticket/QR reference |
| `status` | VARCHAR(32) | | NO | Status (`ISSUED`, `CANCELLED`, `INVALIDATED`) |
| `historical_snapshot` | JSONB | | NO | JSON snapshot of booking details |
| `issued_at` | TIMESTAMPTZ | | NO | Issue time |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

### 8.6 `booking_idempotency_records`

Prevents duplicate booking processing after retries.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `idempotency_record_id` | UUID | PK | NO | Record identifier |
| `user_id` | UUID | | NO | Application reference to User Service |
| `operation_type` | VARCHAR(64) | | NO | Operation name |
| `idempotency_key` | VARCHAR(255) | | NO | Client-provided key |
| `booking_id` | UUID | FK | YES | Resulting booking reference |
| `request_hash` | VARCHAR(128) | | NO | Request fingerprint |
| `status` | VARCHAR(32) | | NO | Status (`IN_PROGRESS`, `COMPLETED`, `FAILED`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraint: `UNIQUE(user_id, operation_type, idempotency_key)`

---

## 9. Payment Database (`payment_db`)

### 9.1 `payments`

Represents the payment associated with a booking.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `payment_id` | UUID | PK | NO | Payment identifier |
| `booking_id` | UUID | | NO | Application reference to Booking Service |
| `user_id` | UUID | | NO | Application reference to User Service |
| `amount` | NUMERIC(12,2) | | NO | Payment amount |
| `currency` | CHAR(3) | | NO | Currency code |
| `status` | VARCHAR(32) | | NO | Status (`INITIATED`, `PENDING`, `SUCCESS`, `FAILURE`, `CANCELLED`, `TIMEOUT`, `UNKNOWN`, `REFUND_PENDING`, `REFUNDED`) |
| `provider_name` | VARCHAR(100) | | YES | Payment provider name |
| `provider_payment_reference` | VARCHAR(255) | | YES | External provider reference |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Unique Index: `UNIQUE(provider_name, provider_payment_reference) WHERE provider_payment_reference IS NOT NULL`

### 9.2 `payment_attempts`

Tracks individual attempts for recovery and auditing.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `payment_attempt_id` | UUID | PK | NO | Attempt identifier |
| `payment_id` | UUID | FK | NO | References `payments.payment_id` |
| `provider_attempt_reference` | VARCHAR(255) | | YES | Provider attempt reference |
| `status` | VARCHAR(32) | | NO | Status (`INITIATED`, `SUCCESS`, `FAILURE`, `CANCELLED`, `TIMEOUT`, `UNKNOWN`) |
| `initiated_at` | TIMESTAMPTZ | | NO | Initiation time |
| `completed_at` | TIMESTAMPTZ | | YES | Completion time |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraint: `UNIQUE(payment_id, provider_attempt_reference)`

### 9.3 `refund_reversals`

Tracks payment reversal/refund work.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `refund_reversal_id` | UUID | PK | NO | Refund/reversal identifier |
| `payment_id` | UUID | FK | NO | References `payments.payment_id` |
| `payment_attempt_id` | UUID | FK | YES | References `payment_attempts.payment_attempt_id` |
| `amount` | NUMERIC(12,2) | | NO | Amount to refund |
| `status` | VARCHAR(32) | | NO | Status (`INITIATED`, `REFUND_PENDING`, `REFUNDED`, `FAILED`, `CANCELLED`) |
| `provider_reference` | VARCHAR(255) | UNIQUE | YES | Provider refund reference |
| `requested_at` | TIMESTAMPTZ | | NO | Request time |
| `completed_at` | TIMESTAMPTZ | | YES | Completion time |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

### 9.4 `payment_reconciliations`

Supports recovery of unknown payment outcomes.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `reconciliation_id` | UUID | PK | NO | Reconciliation record identifier |
| `payment_id` | UUID | FK | NO | References `payments.payment_id` |
| `status` | VARCHAR(32) | | NO | Status (`PENDING`, `RESOLVED`, `FAILED`) |
| `provider_status_observed` | VARCHAR(64) | | YES | Provider-observed state |
| `checked_at` | TIMESTAMPTZ | | NO | Check time |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |

### 9.5 `payment_idempotency_records`

Prevents duplicate payment operation processing after retries.

| Column | Type | Key | Null | Description |
|---|---|---|---|---|
| `idempotency_record_id` | UUID | PK | NO | Record identifier |
| `user_id` | UUID | | NO | Application reference to User Service |
| `operation_type` | VARCHAR(64) | | NO | Operation name |
| `idempotency_key` | VARCHAR(255) | | NO | Client-provided key |
| `payment_id` | UUID | FK | YES | Resulting payment reference |
| `payment_attempt_id` | UUID | FK | YES | Resulting attempt reference |
| `request_hash` | VARCHAR(128) | | NO | Request fingerprint |
| `status` | VARCHAR(32) | | NO | Status (`IN_PROGRESS`, `COMPLETED`, `FAILED`) |
| `created_at` | TIMESTAMPTZ | | NO | Creation time |
| `updated_at` | TIMESTAMPTZ | | NO | Last update |

Constraint: `UNIQUE(user_id, operation_type, idempotency_key)`

---

## 10. Relationship and Cardinality Summary

### Within-Service Relationships

| Parent | Child | Cardinality |
|---|---|---|
| `users` | `user_roles` | 1:N |
| `roles` | `user_roles` | 1:N |
| `users` | `otp_verifications` | 1:N |
| `theatres` | `screens` | 1:N |
| `screens` | `physical_seats` | 1:N |
| `theatres` | `theatre_operator_assignments` | 1:N |
| `shows` | `show_pricing` | 1:N |
| `shows` | `cancellation_policies` | 1:1 |
| `bookings` | `seat_holds` | 1:1 (`seat_holds.booking_id NOT NULL UNIQUE`) |
| `bookings` | `booking_seats` | 1:N |
| `bookings` | `tickets` | 1:1 |
| `bookings` | `booking_idempotency_records` | 1:N |
| `payments` | `payment_attempts` | 1:N |
| `payments` | `refund_reversals` | 1:N |
| `payments` | `payment_reconciliations` | 1:N |
| `payments` | `payment_idempotency_records` | 1:N |

### Cross-Service References (No DB Foreign Keys)

| Source Entity | Referenced Entity | Mechanism |
|---|---|---|
| `theatre_operator_assignments.user_id` | User | Application-level UUID |
| `shows.movie_id` | Movie | Application-level UUID |
| `shows.theatre_id` | Theatre | Application-level UUID |
| `shows.screen_id` | Screen | Application-level UUID |
| `bookings.user_id` | User | Application-level UUID |
| `bookings.show_id` | Show | Application-level UUID |
| `bookings.payment_id` | Payment | Application-level UUID |
| `seat_holds.user_id` | User | Application-level UUID |
| `seat_holds.show_id` | Show | Application-level UUID |
| `show_seat_inventory.show_id` | Show | Application-level UUID |
| `show_seat_inventory.physical_seat_id` | Physical Seat | Application-level UUID |
| `booking_seats.physical_seat_id` | Physical Seat | Application-level UUID |
| `payments.booking_id` | Booking | Application-level UUID |
| `payments.user_id` | User | Application-level UUID |

---

## 11. Logical ERD Diagrams

### USER SERVICE
```text
┌───────────┐       ┌────────────┐       ┌──────────────┐
│   users   │ 1──N  │ user_roles │ N──1  │    roles     │
└─────┬─────┘       └────────────┘       └──────────────┘
      │
      │ 1:N
      ▼
┌──────────────────┐
│ otp_verifications│
└──────────────────┘
```

### THEATRE SERVICE
```text
┌───────────┐ 1──N ┌─────────┐ 1──N ┌───────────────┐
│  theatres │─────│ screens │─────│ physical_seats│
└─────┬─────┘      └─────────┘      └───────────────┘
      │ 1:N
      ▼
┌──────────────────────────────┐
│ theatre_operator_assignments │
└──────────────────────────────┘
```

### SHOW SERVICE
```text
┌────────┐ 1──N ┌─────────────┐
│ shows  │─────│ show_pricing│
└───┬────┘     └─────────────┘
    │ 1:1
    ▼
┌───────────────────────┐
│ cancellation_policies │
└───────────────────────┘
```

### BOOKING SERVICE
```text
┌────────────┐ 1──1 ┌────────────┐ 1──N ┌───────────────┐
│  bookings  │─────│ seat_holds │      │ booking_seats │
└─────┬──────┘      └────────────┘      └───────┬───────┘
      │ 1:1                                     │ N:1
      ▼                                         ▼
┌────────────┐                         ┌──────────────────────┐
│  tickets   │                         │ show_seat_inventory  │
└────────────┘                         └──────────────────────┘
```

### PAYMENT SERVICE
```text
┌────────────┐ 1──N ┌──────────────────┐
│  payments  │─────│ payment_attempts │
└─────┬──────┘     └──────────────────┘
      │
      ├── 1:N ┌──────────────────┐
      │      │ refund_reversals │
      │      └──────────────────┘
      │
      └── 1:N ┌───────────────────────┐
             │ payment_reconciliations │
             └─────────────────────────┘
```

---

## 12. Requirement Traceability

| Requirement | Database Design Support |
|---|---|
| Multi-screen cinemas | `theatres`, `screens`, `physical_seats` |
| Movie management | `movies` |
| Show scheduling | `shows` |
| Seat availability | `show_seat_inventory` |
| Concurrent seat allocation | Unique `(show_id, physical_seat_id)` + atomic inventory acquisition |
| Temporary seat hold | `bookings(INITIATED)` → `seat_holds(ACTIVE)` |
| 5-minute hold | `seat_holds.normal_expires_at` |
| Payment grace | `seat_holds.payment_grace_expires_at`, `bookings.payment_grace_expires_at` |
| Booking persistence | `bookings`, `booking_seats` |
| Ticket generation | `tickets` |
| Role authentication | `users`, `roles`, `user_roles`, `otp_verifications` |
| Theatre operators | `theatre_operator_assignments` |
| Pricing | `show_pricing` |
| Cancellation policy | `cancellation_policies` |
| Idempotency | `booking_idempotency_records`, `payment_idempotency_records` |
| Payment unknown/reconciliation | `payment_reconciliations` |
| Refund/reversal | `refund_reversals` |

---

## 13. Scope Boundary

This ERD defines the logical database model required for Review-1 and the core BookNGo booking workflow, perfectly synchronized with `schema.sql`.
