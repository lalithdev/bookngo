package com.bookngo.bookingservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private UUID refundReversalId;
    private UUID paymentId;
    private BigDecimal amount;
    private String status;
    private String providerReference;
}
