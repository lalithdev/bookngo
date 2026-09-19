package com.bookngo.userservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequestResponse {

    private UUID otpId;
    private String phoneNumber;
    private String status;
    private OffsetDateTime expiresAt;
}
