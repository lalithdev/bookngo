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
public class InternalRefundRequest {

    @NotNull(message = "bookingId must not be null")
    private UUID bookingId;

    @NotBlank(message = "reason must not be blank")
    private String reason;
}
