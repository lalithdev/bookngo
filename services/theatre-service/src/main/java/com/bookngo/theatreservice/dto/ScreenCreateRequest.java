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
public class ScreenCreateRequest {

    @NotBlank(message = "Screen name is required")
    private String name;

    private String screenFormat;
}
