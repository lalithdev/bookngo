package com.bookngo.theatreservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigureSeatsRequest {

    @NotEmpty(message = "Seats list cannot be empty")
    @Valid
    private List<PhysicalSeatCreateRequest> seats;
}
