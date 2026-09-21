package com.bookngo.paymentservice.entity;

public enum PaymentAttemptStatus {
    INITIATED,
    SUCCESS,
    FAILURE,
    CANCELLED,
    TIMEOUT,
    UNKNOWN
}
