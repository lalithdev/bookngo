package com.bookngo.bookingservice.dto;

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
public class PaymentOutcomeRequest {

    @NotNull(message = "paymentId must not be null")
    private UUID paymentId;

    @NotBlank(message = "outcome must not be blank")
    private String outcome; // SUCCESS, FAILURE, CANCELLED, UNKNOWN
}
