package com.bookngo.bookingservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationPolicyDto {
    private UUID cancellationPolicyId;
    private UUID showId;
    private String policyType; // CANCELLABLE, NON_CANCELLABLE
    private OffsetDateTime cancellationDeadline;
    private String status;
}
