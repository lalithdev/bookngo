package com.bookngo.bookingservice.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatHoldRequest {

    @NotEmpty(message = "physicalSeatIds must not be empty")
    @Size(min = 1, max = 6, message = "Cannot hold more than 6 seats in a single booking")
    private List<UUID> physicalSeatIds;
}
