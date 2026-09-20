package com.bookngo.showservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowPricingUpdateRequest {

    @NotBlank(message = "seatCategory is required")
    private String seatCategory;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.00", message = "amount must be greater than or equal to 0")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3-character ISO code")
    private String currency;
}
