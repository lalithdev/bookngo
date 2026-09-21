package com.bookngo.bookingservice.entity;

public enum BookingStatus {
    INITIATED,
    HELD,
    PAYMENT_PENDING,
    CONFIRMED,
    TICKET_ISSUED,
    EXPIRED,
    CANCELLED,
    PAYMENT_FAILED,
    PAYMENT_CANCELLED,
    PAYMENT_UNKNOWN,
    REFUND_PENDING,
    REFUNDED
}
