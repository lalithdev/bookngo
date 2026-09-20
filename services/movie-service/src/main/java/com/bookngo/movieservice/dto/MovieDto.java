package com.bookngo.movieservice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieDto {

    private UUID movieId;
    private String title;
    private String description;
    private String language;
    private List<String> genres;
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private String posterReference;
    private String status;
    private Map<String, Object> metadata;
}
