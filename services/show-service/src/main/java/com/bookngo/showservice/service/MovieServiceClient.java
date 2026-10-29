package com.bookngo.showservice.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bookngo.showservice.client.MovieFeignClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MovieServiceClient {

    private final MovieFeignClient movieFeignClient;

    public MovieServiceClient(MovieFeignClient movieFeignClient) {
        this.movieFeignClient = movieFeignClient;
    }

    /**
     * Resolves movie IDs with the given language using Movie Service's existing GET /api/v1/movies?language={language}.
     */
    public List<UUID> getMovieIdsByLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> movies = movieFeignClient.getMovies(language.trim());

            if (movies == null || movies.isEmpty()) {
                return Collections.emptyList();
            }

            return movies.stream()
                    .map(m -> m.get("movieId"))
                    .filter(id -> id != null)
                    .map(id -> UUID.fromString(id.toString()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to query Movie Service for language '{}': {}", language, e.getMessage());
            return Collections.emptyList();
        }
    }
}
