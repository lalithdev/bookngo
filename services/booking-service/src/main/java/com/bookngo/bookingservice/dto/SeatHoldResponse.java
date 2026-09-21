package com.bookngo.bookingservice.dto;

import java.math.BigDecimal;
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
public class SeatHoldResponse {
    private UUID holdId;
    private UUID bookingId;
    private UUID showId;
    private String status;
    private OffsetDateTime normalExpiresAt;
    private OffsetDateTime paymentGraceExpiresAt;
    private BigDecimal totalAmount;
    private String currency;
}
