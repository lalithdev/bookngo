package com.bookngo.showservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.bookngo.showservice.entity.ShowStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponse {
    private UUID showId;
    private UUID movieId;
    private UUID theatreId;
    private UUID screenId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String showFormat;
    private ShowStatus status;
}
