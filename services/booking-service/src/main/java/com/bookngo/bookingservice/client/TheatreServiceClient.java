package com.bookngo.bookingservice.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bookngo.bookingservice.dto.PhysicalSeatDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TheatreServiceClient {

    private final RestClient restClient;

    public TheatreServiceClient(
            @Value("${services.theatre-service.url:http://localhost:8083}") String theatreServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(theatreServiceUrl)
                .build();
    }

    public List<PhysicalSeatDto> getPhysicalSeatsByScreen(UUID screenId) {
        if (screenId == null) {
            return Collections.emptyList();
        }

        try {
            List<PhysicalSeatDto> seats = restClient.get()
                    .uri("/api/v1/screens/{screenId}/seats", screenId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PhysicalSeatDto>>() {});

            return seats != null ? seats : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch physical seats for screen {}: {}", screenId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
