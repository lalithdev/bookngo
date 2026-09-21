package com.bookngo.paymentservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {

    @NotNull(message = "bookingId must not be null")
    private UUID bookingId;

    @NotNull(message = "providerName must not be null")
    private String providerName;

    // Amount and currency are intentionally omitted:
    // Payment Service fetches authoritative amount from Booking Service.
}
