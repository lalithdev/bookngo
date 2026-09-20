package com.bookngo.theatreservice.dto;

import java.util.Map;
import java.util.UUID;

import com.bookngo.theatreservice.entity.TheatreStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreResponse {
    private UUID theatreId;
    private String name;
    private String address;
    private String city;
    private Map<String, Object> locationMetadata;
    private TheatreStatus status;
}
