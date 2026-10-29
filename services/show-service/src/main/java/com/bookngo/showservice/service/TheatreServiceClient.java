package com.bookngo.showservice.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bookngo.showservice.client.TheatreFeignClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TheatreServiceClient {

    private final TheatreFeignClient theatreFeignClient;

    public TheatreServiceClient(TheatreFeignClient theatreFeignClient) {
        this.theatreFeignClient = theatreFeignClient;
    }

    /**
     * Resolves theatre IDs located in the given city using Theatre Service's existing GET /api/v1/theatres?city={city}.
     */
    public List<UUID> getTheatreIdsByCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> theatres = theatreFeignClient.getTheatres(city.trim());

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
