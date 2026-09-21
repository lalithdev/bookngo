package com.bookngo.paymentservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.bookngo.paymentservice.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private UUID paymentId;
    private UUID bookingId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String providerPaymentReference;
    private OffsetDateTime paymentGraceExpiresAt;
}
