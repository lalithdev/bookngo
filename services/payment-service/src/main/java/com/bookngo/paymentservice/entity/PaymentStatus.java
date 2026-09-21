package com.bookngo.paymentservice.entity;

public enum PaymentStatus {
    INITIATED,
    PENDING,
    SUCCESS,
    FAILURE,
    CANCELLED,
    TIMEOUT,
    UNKNOWN,
    REFUND_PENDING,
    REFUNDED
}
