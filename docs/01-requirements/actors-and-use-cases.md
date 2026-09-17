# Actors and Use Cases

## Actors

## 1. Customer

The Customer uses the platform to discover shows and purchase tickets.

The customer can:

- Browse movies.
- View movie details.
- Select a location.
- Filter shows.
- Select a theatre.
- Select a show.
- View seat availability.
- Select seats.
- Request a seat hold.
- Authenticate using phone number and OTP.
- Create a booking.
- Initiate payment.
- View booking status.
- View digital tickets.
- View booking history.
- View payment history.
- Cancel eligible bookings.
- View refund status.

---

## 2. Theatre Operator

A Theatre Operator manages assigned theatre resources.

The operator can:

- Manage assigned theatres.
- Manage screens.
- Configure physical seats.
- Create and update shows.
- Configure show pricing.
- Configure cancellation policy.
- View bookings.
- View occupancy.

The operator must not manage theatres that are not assigned to them.

---

## 3. Platform Administrator

The Platform Administrator performs platform-level administration.

The administrator can:

- Manage platform users.
- Manage theatre/operator accounts.
- Manage movies.
- Monitor platform operations.
- Manage system-level configuration.

The Review 1 implementation does not require a large enterprise-grade
administration panel.

---

## 4. External Payment Provider

The Payment Provider represents the external payment boundary.

It may provide:

- Payment initiation.
- Payment processing.
- Payment success/failure.
- Payment cancellation.
- Payment timeout.
- Payment status.
- Refund/reversal processing.

For Review 1, this actor may be simulated.

---

## 5. OTP/SMS Provider

The OTP/SMS Provider represents the external messaging boundary.

It may provide:

- OTP delivery.
- Delivery status.

For Review 1, OTP delivery may be simulated.

---

# Customer Use Cases

## UC-01 — Browse Movies

Customer views movies available for booking.

## UC-02 — Select Location

Customer selects a city or supported location.

Browser-based geolocation may be provided as a convenience but is not required
for booking.

## UC-03 — View Movie Details

Customer views movie information.

## UC-04 — Filter Movies/Shows

Customer filters available shows by:

- Location
- Date
- Theatre
- Language
- Format
- Show time

## UC-05 — Select Theatre

Customer selects a theatre offering the required show.

## UC-06 — Select Show

Customer selects a scheduled show.

## UC-07 — View Seat Map

Customer views the seating layout and current show-specific seat states.

## UC-08 — Select Seats

Customer selects seats.

A maximum of six seats may be included in one booking attempt.

Selecting a seat in the UI does not itself create a server-side hold.

## UC-09 — Hold Seats

Customer confirms the seat selection.

The server attempts to create a temporary hold.

A successful hold lasts five minutes.

## UC-10 — Authenticate via OTP

Customer authenticates using phone number and OTP.

## UC-11 — Initiate Booking

Customer creates a booking for successfully held seats.

## UC-12 — Make Payment

Customer initiates payment for the booking while the hold remains valid.

## UC-13 — Confirm Booking

The system confirms the booking after the required payment condition has been
successfully satisfied.

## UC-14 — Generate/View Ticket

The system generates a digital ticket after successful confirmation.

## UC-15 — View My Bookings

Customer views booking history and booking status.

## UC-16 — View Payment History

Customer views payment transactions associated with their bookings.

## UC-17 — Cancel Booking

Customer cancels a booking when the show's cancellation policy permits it.

## UC-18 — View Refund Status

Customer views the state of a refund/reversal where applicable.

---

# Theatre Operator Use Cases

## UC-19 — Manage Theatre

Operator creates or updates assigned theatre information.

## UC-20 — Manage Screens

Operator manages screens belonging to assigned theatres.

## UC-21 — Configure Seats

Operator configures physical seats belonging to screens.

## UC-22 — Manage Shows

Operator creates and updates scheduled shows.

A show uses one screen.

## UC-23 — Configure Ticket Pricing

Operator configures show pricing using supported seat categories.

## UC-24 — View Bookings

Operator views bookings for assigned theatres/shows.

## UC-25 — View Occupancy

Operator views occupancy information derived from show booking/inventory data.

## UC-26 — Configure Cancellation Policy

Operator configures whether a show is:

- CANCELLABLE
- NON_CANCELLABLE

For cancellable shows, the operator may configure a cancellation deadline.

---

# Platform Administrator Use Cases

## UC-27 — Manage Movies

Administrator manages movie information available on the platform.

## UC-28 — Manage Users/Theatre Accounts

Administrator manages platform users and theatre/operator accounts.

## UC-29 — Monitor Platform

Administrator views operational information required for platform oversight.

---

# External Interaction Use Cases

Payment and OTP are external boundaries.

The system must not assume that an external provider always responds
successfully.

The design must account for:

- Timeout.
- Failure.
- Duplicate callbacks.
- Network failure.
- Unknown payment state.
- Recovery.