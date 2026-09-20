package com.bookngo.movieservice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Language is required")
    private String language;

    @NotEmpty(message = "At least one genre is required")
    private List<String> genres;

    @NotNull(message = "Duration in minutes is required")
    @Positive(message = "Duration must be greater than 0")
    private Integer durationMinutes;

    private LocalDate releaseDate;

    private String posterReference;

    private Map<String, Object> metadata;
}
