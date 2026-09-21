package com.bookngo.paymentservice.dto;

import java.util.UUID;

import com.bookngo.paymentservice.entity.PaymentStatus;
import com.bookngo.paymentservice.entity.ReconciliationStatus;
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
public class PaymentReconciliationResponse {
    private UUID reconciliationId;
    private UUID paymentId;
    private ReconciliationStatus status;
    private String providerStatusObserved;
    private PaymentStatus resolvedPaymentStatus;
}
