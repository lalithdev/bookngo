package com.bookngo.showservice.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MovieServiceClient {

    private final RestClient restClient;

    public MovieServiceClient(
            @Value("${services.movie-service.url:http://localhost:8082}") String movieServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(movieServiceUrl)
                .build();
    }

    /**
     * Resolves movie IDs with the given language using Movie Service's existing GET /api/v1/movies?language={language}.
     */
    public List<UUID> getMovieIdsByLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> movies = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/movies")
                            .queryParam("language", language.trim())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

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
