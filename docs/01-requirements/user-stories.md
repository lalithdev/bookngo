# User Stories

## Discovery

### US-01 — Browse Movies

As a customer,
I want to browse currently available movies,
so that I can choose a movie to watch.

### US-02 — Select Location

As a customer,
I want to select my location,
so that I can see relevant theatres and shows.

### US-03 — Filter Shows

As a customer,
I want to filter shows by language, format, date, theatre, and time,
so that I can find a suitable screening.

---

## Seat Selection

### US-04 — View Seat Availability

As a customer,
I want to view the current seat availability for a show,
so that I can select available seats.

### US-05 — Temporarily Hold Seats

As a customer,
I want selected seats to be temporarily held when I proceed with booking,
so that another customer cannot acquire them during my transaction.

### US-06 — Receive Seat Availability Updates

As a customer,
I want seat availability to update when another customer holds or books a
seat,
so that I do not intentionally attempt to acquire unavailable inventory.

---

## Booking

### US-07 — Book Multiple Seats

As a customer,
I want to book up to six seats for a show,
so that I can purchase tickets for myself and my group.

### US-08 — Prevent Double Booking

As a customer,
I want the system to prevent another customer from successfully acquiring
the same show-seat,
so that my booking remains valid.

### US-09 — Recover From Request Failure

As a customer,
I want a lost response after a booking or payment operation to be recoverable,
so that I do not have to make another payment or accidentally create another
booking.

---

## Authentication

### US-10 — Authenticate Using OTP

As a customer,
I want to authenticate using my phone number and OTP,
so that booking functionality is protected.

---

## Payment

### US-11 — Pay for Booking

As a customer,
I want to pay for my booking,
so that my held seats can be confirmed.

### US-12 — Handle Failed Payment

As a customer,
I want failed or cancelled payments to avoid confirming my booking,
so that incorrect bookings are not created.

### US-13 — Handle Payment Timeout

As a customer,
I want the system to show an appropriate status when payment cannot be
confirmed,
so that I know whether I should wait, retry, or check the payment status.

### US-14 — Avoid Duplicate Payment

As a customer,
I want retrying the same payment operation not to charge me twice,
so that network failures do not create duplicate transactions.

---

## Ticket

### US-15 — Receive Digital Ticket

As a customer,
I want to receive a digital ticket after successful booking confirmation,
so that I can use the ticket for the show.

---

## Cancellation

### US-16 — View Cancellation Policy

As a customer,
I want to know whether a show is cancellable before booking,
so that I understand the cancellation conditions.

### US-17 — Cancel Eligible Booking

As a customer,
I want to cancel my booking when the show's cancellation policy permits it,
so that I can receive the applicable refund/reversal.

---

## Theatre Management

### US-18 — Manage Theatre Resources

As a theatre operator,
I want to manage my theatres, screens, and physical seats,
so that the system represents my actual seating inventory.

### US-19 — Manage Shows

As a theatre operator,
I want to create and update shows,
so that customers can book scheduled screenings.

### US-20 — Configure Pricing

As a theatre operator,
I want to configure ticket prices by supported seat category,
so that customers are charged the applicable amount.

### US-21 — Configure Cancellation Policy

As a theatre operator,
I want to configure cancellation availability and deadlines,
so that customer cancellation follows theatre policy.

### US-22 — View Occupancy

As a theatre operator,
I want to view show occupancy,
so that I can monitor ticket utilization.

---

## Administration

### US-23 — Manage Movies

As a platform administrator,
I want to manage movies,
so that correct movie information is available to customers.

### US-24 — Manage Theatre Accounts

As a platform administrator,
I want to manage theatre/operator accounts,
so that access to theatre resources is controlled.

---

## Concurrency

### US-25 — Protect Concurrent Seat Allocation

As a platform operator,
I want concurrent requests for the same show-seat to be resolved safely,
so that double booking never occurs.

### US-26 — Automatically Release Expired Holds

As a platform operator,
I want expired holds to release automatically,
so that temporary failures do not permanently block inventory.

### US-27 — Recover From Payment Success

As a platform operator,
I want successful payments to remain recoverable even when confirmation or
network failures occur,
so that the system does not lose payment state or require an unnecessary
second payment.