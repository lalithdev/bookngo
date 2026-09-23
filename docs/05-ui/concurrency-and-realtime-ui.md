# Frontend Concurrency and Realtime UI

## Server Authority

The frontend must never treat its local seat state as authoritative.

The server is authoritative for:

- AVAILABLE
- HELD
- BOOKED
- UNAVAILABLE
- hold expiration
- booking confirmation

## Seat Selection

Selecting a seat locally does not create a hold.

A hold is created only after the user chooses
"Proceed to Book".

## Hold

Creating a hold requires an Idempotency-Key.

The same logical hold attempt must reuse the same key
when a request is retried.

## Seat Inventory

Seat inventory should be periodically refreshed while the
seat-selection screen is active.

The UI must handle the possibility that a seat displayed as
available becomes unavailable before the hold request.

The frontend must display the server response rather than
assuming the local state was correct.

## Timer

The countdown is informational.

The frontend must not determine whether a hold is valid
based solely on the browser timer.

The server's expiry timestamp/state is authoritative.

## Payment

Payment status is server-authoritative.

While payment is pending, the frontend may poll the payment
status endpoint.

Polling stops when a terminal state is received.

## No Optimistic Booking

The frontend must not show:

"Booking confirmed"

until the backend confirms the booking.