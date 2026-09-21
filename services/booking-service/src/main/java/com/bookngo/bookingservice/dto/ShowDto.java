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
public class ShowDto {
    private UUID showId;
    private UUID movieId;
    private UUID theatreId;
    private UUID screenId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String showFormat;
    private String status;
}
