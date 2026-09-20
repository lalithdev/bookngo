package com.bookngo.showservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.bookngo.showservice.entity.PolicyStatus;
import com.bookngo.showservice.entity.PolicyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyResponse {
    private UUID cancellationPolicyId;
    private UUID showId;
    private PolicyType policyType;
    private OffsetDateTime cancellationDeadline;
    private PolicyStatus status;
}
