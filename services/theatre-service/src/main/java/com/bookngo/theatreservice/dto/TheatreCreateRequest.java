package com.bookngo.theatreservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreCreateRequest {

    @NotBlank(message = "Theatre name is required")
    private String name;

    @NotBlank(message = "Theatre address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;
}
