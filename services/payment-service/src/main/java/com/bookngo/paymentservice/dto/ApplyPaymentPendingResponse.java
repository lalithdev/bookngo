package com.bookngo.paymentservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO received FROM Booking Service: response from
 * POST /internal/v1/bookings/{bookingId}/apply-payment-pending
 * Contains authoritative amount/currency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyPaymentPendingResponse {
    private UUID bookingId;
    private BigDecimal totalAmount;
    private String currency;
    private String status; // BookingStatus as string
    private OffsetDateTime paymentGraceExpiresAt;
}
