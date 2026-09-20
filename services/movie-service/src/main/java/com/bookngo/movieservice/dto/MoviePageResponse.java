package com.bookngo.movieservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoviePageResponse {

    private List<MovieDto> content;
    private long totalElements;
    private int totalPages;
}
