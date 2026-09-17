-- BookNGo PostgreSQL schema
-- Run each labelled section only against that service's own database.
-- This file deliberately contains no CREATE DATABASE statements, cross-service
-- foreign keys, shared tables, or distributed-transaction mechanism.
-- UUID generation requires pgcrypto in each business database.
--
-- PostgreSQL 14+ is assumed for generated UUID support through pgcrypto.

-- ============================================================================
-- USER SERVICE DATABASE: user_db
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(320),
    password_hash VARCHAR(255) NULL, -- Nullable: CUSTOMER uses phone+OTP (NULL); THEATRE_OPERATOR & ADMIN require password.
    account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (account_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    role_code VARCHAR(32) PRIMARY KEY
        CHECK (role_code IN ('CUSTOMER', 'THEATRE_OPERATOR', 'ADMIN'))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(user_id),
    role_code VARCHAR(32) NOT NULL REFERENCES roles(role_code),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_code)
);

CREATE TABLE otp_verifications (
    otp_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    phone_number VARCHAR(32) NOT NULL,
    otp_secret_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'REJECTED', 'LOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    rate_limit_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CHECK (expires_at > created_at),
    CHECK (verified_at IS NULL OR verified_at >= created_at)
);

CREATE INDEX idx_otp_verifications_phone_expiry
    ON otp_verifications (phone_number, expires_at DESC);

-- ============================================================================
-- MOVIE SERVICE DATABASE: movie_db
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE movies (
    movie_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    language VARCHAR(64) NOT NULL,
    genres JSONB NOT NULL DEFAULT '[]'::jsonb,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    release_date DATE,
    poster_reference TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movies_status ON movies (status);
CREATE INDEX idx_movies_language ON movies (language);

-- ============================================================================
-- THEATRE SERVICE DATABASE: theatre_db
-- user_id below is an application-level reference to User Service.
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE theatres (
    theatre_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(128) NOT NULL,
    location_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE screens (
    screen_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    theatre_id UUID NOT NULL REFERENCES theatres(theatre_id),
    name VARCHAR(100) NOT NULL,
    screen_format VARCHAR(64),
    configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_screens_theatre_name UNIQUE (theatre_id, name)
);

CREATE TABLE physical_seats (
    physical_seat_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id UUID NOT NULL REFERENCES screens(screen_id),
    row_label VARCHAR(20) NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    seat_category VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_physical_seats_screen_position
        UNIQUE (screen_id, row_label, seat_number)
);

CREATE TABLE theatre_operator_assignments (
    assignment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    theatre_id UUID NOT NULL REFERENCES theatres(theatre_id),
    user_id UUID NOT NULL, -- External User Service reference; no FK.
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_theatre_operator_assignments UNIQUE (theatre_id, user_id)
);

CREATE INDEX idx_screens_theatre ON screens (theatre_id);
CREATE INDEX idx_physical_seats_screen ON physical_seats (screen_id);

-- ============================================================================
-- SHOW SERVICE DATABASE: show_db
-- movie_id, theatre_id, and screen_id are external application references.
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE shows (
    show_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,   -- Movie Service reference; no FK.
    theatre_id UUID NOT NULL, -- Theatre Service reference; no FK.
    screen_id UUID NOT NULL,  -- Theatre Service reference; no FK.
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    show_format VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('DRAFT', 'SCHEDULED', 'CANCELLED', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (ends_at > starts_at)
);

CREATE TABLE show_pricing (
    show_pricing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID NOT NULL REFERENCES shows(show_id),
    seat_category VARCHAR(32) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cancellation_policies (
    cancellation_policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID NOT NULL REFERENCES shows(show_id),
    policy_type VARCHAR(32) NOT NULL
        CHECK (policy_type IN ('CANCELLABLE', 'NON_CANCELLABLE')),
    cancellation_deadline TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cancellation_policies_show UNIQUE (show_id),
    CHECK (
        (policy_type = 'CANCELLABLE' AND cancellation_deadline IS NOT NULL)
        OR
        (policy_type = 'NON_CANCELLABLE' AND cancellation_deadline IS NULL)
    )
);

CREATE UNIQUE INDEX uq_show_pricing_active_category
    ON show_pricing (show_id, seat_category)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_shows_discovery
    ON shows (movie_id, theatre_id, starts_at)
    WHERE status = 'SCHEDULED';

-- ============================================================================
-- BOOKING SERVICE DATABASE: booking_db
-- user_id, show_id, physical_seat_id, and payment_id are external references.
-- No foreign key in this section crosses a service database.
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE bookings (
    booking_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL, -- User Service reference; no FK.
    show_id UUID NOT NULL, -- Show Service reference; no FK.
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INITIATED'
        CHECK (status IN (
            'INITIATED', 'HELD', 'PAYMENT_PENDING', 'CONFIRMED',
            'TICKET_ISSUED', 'EXPIRED', 'CANCELLED', 'PAYMENT_FAILED',
            'PAYMENT_CANCELLED', 'PAYMENT_UNKNOWN', 'REFUND_PENDING', 'REFUNDED'
        )),
    payment_grace_expires_at TIMESTAMPTZ,
    payment_id UUID, -- Payment Service reference; no FK.
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bookings_id_user_show UNIQUE (booking_id, user_id, show_id)
);

CREATE TABLE seat_holds (
    hold_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(booking_id),
    user_id UUID NOT NULL, -- User Service reference; no FK.
    show_id UUID NOT NULL, -- Show Service reference; no FK.
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'RELEASED', 'CONVERTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    normal_expires_at TIMESTAMPTZ NOT NULL,
    payment_grace_expires_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_holds_booking_owner_show
        FOREIGN KEY (booking_id, user_id, show_id)
        REFERENCES bookings (booking_id, user_id, show_id),
    CONSTRAINT uq_seat_holds_id_show UNIQUE (hold_id, show_id),
    CHECK (normal_expires_at > created_at),
    CHECK (
        payment_grace_expires_at IS NULL
        OR payment_grace_expires_at > normal_expires_at
    )
);

CREATE TABLE show_seat_inventory (
    show_seat_inventory_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID NOT NULL,          -- Show Service reference; no FK.
    physical_seat_id UUID NOT NULL, -- Theatre Service reference; no FK.
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED')),
    active_hold_id UUID,
    current_booking_id UUID REFERENCES bookings(booking_id),
    hold_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_show_seat_inventory_show_seat
        UNIQUE (show_id, physical_seat_id)
);

-- The FK is added after both sides of the local circular relationship exist.
ALTER TABLE show_seat_inventory
    ADD CONSTRAINT fk_show_seat_inventory_active_hold
    FOREIGN KEY (active_hold_id, show_id) REFERENCES seat_holds(hold_id, show_id)
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE show_seat_inventory
    ADD CONSTRAINT chk_show_seat_inventory_state
    CHECK (
        (status = 'AVAILABLE'
            AND active_hold_id IS NULL
            AND current_booking_id IS NULL
            AND hold_expires_at IS NULL)
        OR
        (status = 'HELD'
            AND active_hold_id IS NOT NULL
            AND current_booking_id IS NULL
            AND hold_expires_at IS NOT NULL)
        OR
        (status = 'BOOKED'
            AND active_hold_id IS NULL
            AND current_booking_id IS NOT NULL
            AND hold_expires_at IS NULL)
    );

CREATE TABLE booking_seats (
    booking_seat_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(booking_id),
    show_seat_inventory_id UUID NOT NULL
        REFERENCES show_seat_inventory(show_seat_inventory_id),
    physical_seat_id UUID NOT NULL, -- Theatre Service reference; no FK.
    seat_label_snapshot VARCHAR(64) NOT NULL,
    seat_category_snapshot VARCHAR(32) NOT NULL,
    unit_price_snapshot NUMERIC(12, 2) NOT NULL CHECK (unit_price_snapshot >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_booking_seats_booking_physical_seat
        UNIQUE (booking_id, physical_seat_id)
);

CREATE TABLE tickets (
    ticket_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(booking_id),
    ticket_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ISSUED'
        CHECK (status IN ('ISSUED', 'CANCELLED', 'INVALIDATED')),
    historical_snapshot JSONB NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_idempotency_records (
    idempotency_record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL, -- User Service reference; no FK.
    operation_type VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    booking_id UUID REFERENCES bookings(booking_id),
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_booking_idempotency_scope
        UNIQUE (user_id, operation_type, idempotency_key)
);

CREATE INDEX idx_bookings_user_created_at ON bookings (user_id, created_at DESC);
CREATE INDEX idx_bookings_show_status ON bookings (show_id, status);
CREATE INDEX idx_seat_holds_expiry ON seat_holds (status, normal_expires_at)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_show_seat_inventory_show ON show_seat_inventory (show_id);
CREATE INDEX idx_show_seat_inventory_expiry ON show_seat_inventory (hold_expires_at)
    WHERE status = 'HELD';

-- ============================================================================
-- PAYMENT SERVICE DATABASE: payment_db
-- booking_id and user_id are external application references.
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL, -- Booking Service reference; no FK.
    user_id UUID NOT NULL,    -- User Service reference; no FK.
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INITIATED'
        CHECK (status IN ('INITIATED', 'PENDING', 'SUCCESS', 'FAILURE', 'CANCELLED', 'TIMEOUT', 'UNKNOWN', 'REFUND_PENDING', 'REFUNDED')),
    provider_name VARCHAR(100),
    provider_payment_reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_attempts (
    payment_attempt_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(payment_id),
    provider_attempt_reference VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'INITIATED'
        CHECK (status IN ('INITIATED', 'SUCCESS', 'FAILURE', 'CANCELLED', 'TIMEOUT', 'UNKNOWN')),
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (completed_at IS NULL OR completed_at >= initiated_at),
    CONSTRAINT uq_payment_attempt_provider_reference
        UNIQUE (payment_id, provider_attempt_reference)
);

CREATE TABLE refund_reversals (
    refund_reversal_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(payment_id),
    payment_attempt_id UUID REFERENCES payment_attempts(payment_attempt_id),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(32) NOT NULL DEFAULT 'INITIATED'
        CHECK (status IN ('INITIATED', 'REFUND_PENDING', 'REFUNDED', 'FAILED', 'CANCELLED')),
    provider_reference VARCHAR(255) UNIQUE,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (completed_at IS NULL OR completed_at >= requested_at)
);

CREATE TABLE payment_reconciliations (
    reconciliation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(payment_id),
    status VARCHAR(32) NOT NULL
        CHECK (status IN ('PENDING', 'RESOLVED', 'FAILED')),
    provider_status_observed VARCHAR(64),
    checked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_idempotency_records (
    idempotency_record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL, -- User Service reference; no FK.
    operation_type VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    payment_id UUID REFERENCES payments(payment_id),
    payment_attempt_id UUID REFERENCES payment_attempts(payment_attempt_id),
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_idempotency_scope
        UNIQUE (user_id, operation_type, idempotency_key)
);

CREATE UNIQUE INDEX uq_payments_provider_reference
    ON payments (provider_name, provider_payment_reference)
    WHERE provider_payment_reference IS NOT NULL;
CREATE INDEX idx_payments_booking_id ON payments (booking_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_reconciliations_pending ON payment_reconciliations (payment_id, checked_at)
    WHERE status = 'PENDING';
