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
public class BookingSeatResponse {
    private UUID bookingSeatId;
    private UUID physicalSeatId;
    private String seatLabelSnapshot;
    private String seatCategorySnapshot;
    private BigDecimal unitPriceSnapshot;
}
