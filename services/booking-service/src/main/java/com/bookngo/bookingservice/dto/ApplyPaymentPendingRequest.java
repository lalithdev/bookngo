package com.bookngo.bookingservice.dto;

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
public class ApplyPaymentPendingRequest {

    @NotNull(message = "userId must not be null")
    private UUID userId;
}
