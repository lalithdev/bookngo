# Product Definition

## Product Name

**BookNGo**

## Product Type

High-Concurrency Movie Ticket Booking and Seat Allocation Platform.

## Product Objective

BookNGo enables customers to discover movies, theatres, and shows, select
available seats, temporarily hold those seats, complete payment, and receive
digital tickets.

The primary engineering objective is to maintain correct seat inventory under
high concurrent traffic.

## Primary Objective

The system shall support approximately 1,000 concurrent
users/booking requests under defined test conditions while ensuring that:

> A physical seat cannot be successfully confirmed for more than one customer
> for the same show.

## Core Domain Concept

A physical seat belongs permanently to a screen.

The domain hierarchy is:

Theatre
→ Screen
→ Physical Seat

A Show uses one Screen.

Seat availability is therefore show-specific:

Show
→ Screen
→ Physical Seat
→ Show-specific inventory state

The authoritative inventory identity is:

(show_id, physical_seat_id)

The same physical seat may be booked independently for different shows.

## Customer Booking Flow

The intended customer flow is:

Movie Discovery
→ Location
→ Theatre
→ Show
→ Seat Map
→ Seat Selection
→ Server-side Seat Hold
→ Payment
→ Booking Confirmation
→ Digital Ticket

## Seat Hold

Selecting seats in the frontend does not create a server-side hold.

A hold begins only after the backend successfully accepts the requested
seat allocation.

The normal hold duration is:

**5 minutes**

The server-side expiry time is authoritative.

The frontend countdown is only a visual representation.

## Payment Processing

Payment must be initiated while the seat hold is valid.

If payment is initiated before the 5-minute hold expires, the booking may enter
PAYMENT_PENDING.

A maximum additional payment-processing grace period of:

**2 minutes**

may be used.

This grace period exists to handle payment-processing and network delays.

It must not create indefinite seat locking.

## Payment Outcomes

The system supports:

- SUCCESS
- FAILURE
- CANCELLED
- TIMEOUT
- UNKNOWN

An UNKNOWN payment must not automatically trigger another charge.

The payment state must remain recoverable through a payment-status or
reconciliation mechanism.

## Payment and Booking Recovery

If payment succeeds but booking confirmation cannot be completed because of a
downstream failure:

- The successful payment state must be preserved.
- The booking must not falsely appear confirmed.
- Appropriate refund/reversal processing must be initiated.
- The customer must receive an appropriate status.

For Review 1, this recovery may be simulated.

## Booking Cancellation

Each show has a cancellation policy:

- CANCELLABLE
- NON_CANCELLABLE

For cancellable shows, the theatre/operator configures a cancellation
deadline.

The customer may cancel only when the policy permits cancellation.

## Pricing

Review 1 uses simple show pricing based on seat categories such as:

- Regular
- Premium
- VIP

Complex dynamic pricing, coupon systems, and advanced pricing engines are
outside the initial scope.

## Authentication

Customers authenticate using:

Phone Number
→ OTP
→ Verification
→ Authenticated Session

JWT is used to secure authenticated APIs.

## External Integrations

The platform may integrate with:

- Payment Provider
- OTP/SMS Provider

For Review 1 these may be simulated.

## Technical Priorities

The product prioritizes:

1. Seat allocation correctness.
2. Concurrency control.
3. Safe failure handling.
4. Idempotent booking/payment operations.
5. Authentication and authorization.
6. Service isolation.
7. Service discovery.
8. Centralized API routing.
9. Demonstrable scalability testing.

## Cost Constraint

The implementation targets ₹0 total project cost.

## Review 1 Success Condition

The system should be able to demonstrate:

- Customer authentication.
- Movie/show discovery.
- Seat availability.
- Seat hold.
- Concurrent seat contention.
- No double booking.
- Booking.
- Payment simulation.
- Confirmation.
- Ticket generation.
- Cancellation policy.
- JWT protection.
- API Gateway routing.
- Eureka service discovery.