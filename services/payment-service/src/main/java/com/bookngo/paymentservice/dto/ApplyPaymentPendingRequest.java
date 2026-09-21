package com.bookngo.paymentservice.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO sent TO Booking Service: POST /internal/v1/bookings/{bookingId}/apply-payment-pending
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyPaymentPendingRequest {
    private UUID userId;
}
