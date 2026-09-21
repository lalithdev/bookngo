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
public class SeatInventoryItem {
    private UUID showSeatInventoryId;
    private UUID physicalSeatId;
    private String status;
    private OffsetDateTime holdExpiresAt;
}
