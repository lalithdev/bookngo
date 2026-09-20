package com.bookngo.theatreservice.dto;

import java.util.UUID;

import com.bookngo.theatreservice.entity.PhysicalSeatStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicalSeatResponse {
    private UUID physicalSeatId;
    private UUID screenId;
    private String rowLabel;
    private String seatNumber;
    private String seatCategory;
    private PhysicalSeatStatus status;
}
