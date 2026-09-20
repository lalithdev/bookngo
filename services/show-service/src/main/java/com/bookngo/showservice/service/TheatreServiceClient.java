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
public class TheatreServiceClient {

    private final RestClient restClient;

    public TheatreServiceClient(
            @Value("${services.theatre-service.url:http://localhost:8083}") String theatreServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(theatreServiceUrl)
                .build();
    }

    /**
     * Resolves theatre IDs located in the given city using Theatre Service's existing GET /api/v1/theatres?city={city}.
     */
    public List<UUID> getTheatreIdsByCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> theatres = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/theatres")
                            .queryParam("city", city.trim())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (theatres == null || theatres.isEmpty()) {
                return Collections.emptyList();
            }

            return theatres.stream()
                    .map(t -> t.get("theatreId"))
                    .filter(id -> id != null)
                    .map(id -> UUID.fromString(id.toString()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to query Theatre Service for city '{}': {}", city, e.getMessage());
            return Collections.emptyList();
        }
    }
}
