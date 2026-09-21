package com.bookngo.bookingservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowPricingDto {
    private UUID showPricingId;
    private UUID showId;
    private String seatCategory;
    private BigDecimal amount;
    private String currency;
    private String status;
}
