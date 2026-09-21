package com.bookngo.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.bookngo.paymentservice.entity.RefundReversalStatus;
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
public class RefundReversalResponse {
    private UUID refundReversalId;
    private UUID paymentId;
    private BigDecimal amount;
    private RefundReversalStatus status;
    private String providerReference;
}
