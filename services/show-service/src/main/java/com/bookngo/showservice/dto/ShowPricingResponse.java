package com.bookngo.showservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.bookngo.showservice.entity.PricingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowPricingResponse {
    private UUID showPricingId;
    private UUID showId;
    private String seatCategory;
    private BigDecimal amount;
    private String currency;
    private PricingStatus status;
}
