package com.bookngo.paymentservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderCallbackRequest {

    @NotNull(message = "paymentId must not be null")
    private UUID paymentId;

    @NotBlank(message = "providerPaymentReference must not be blank")
    private String providerPaymentReference;

    @NotBlank(message = "providerStatus must not be blank")
    private String providerStatus; // SUCCESS | FAILURE | CANCELLED | UNKNOWN
}
