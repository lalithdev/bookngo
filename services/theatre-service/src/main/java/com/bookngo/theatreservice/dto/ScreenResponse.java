package com.bookngo.theatreservice.dto;

import java.util.UUID;

import com.bookngo.theatreservice.entity.ScreenStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenResponse {
    private UUID screenId;
    private UUID theatreId;
    private String name;
    private String screenFormat;
    private ScreenStatus status;
}
