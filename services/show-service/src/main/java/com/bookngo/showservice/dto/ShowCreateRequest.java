package com.bookngo.showservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowCreateRequest {

    @NotNull(message = "movieId is required")
    private UUID movieId;

    @NotNull(message = "theatreId is required")
    private UUID theatreId;

    @NotNull(message = "screenId is required")
    private UUID screenId;

    @NotNull(message = "startsAt is required")
    private OffsetDateTime startsAt;

    @NotNull(message = "endsAt is required")
    private OffsetDateTime endsAt;

    private String showFormat;
}
