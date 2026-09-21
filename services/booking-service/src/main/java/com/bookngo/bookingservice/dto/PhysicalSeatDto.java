package com.bookngo.bookingservice.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicalSeatDto {
    private UUID physicalSeatId;
    private UUID screenId;
    private String rowLabel;
    private String seatNumber;
    private String seatCategory;
    private String status;
}
