package com.bookngo.showservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurePricingRequest {

    @NotEmpty(message = "pricing list cannot be empty")
    @Valid
    private List<ShowPricingUpdateRequest> pricing;
}
