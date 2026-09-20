package com.bookngo.showservice.dto;

import java.time.OffsetDateTime;

import com.bookngo.showservice.entity.PolicyType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyUpdateRequest {

    @NotNull(message = "policyType is required")
    private PolicyType policyType;

    private OffsetDateTime cancellationDeadline;
}
