package com.bookngo.bookingservice.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bookngo.bookingservice.dto.CancellationPolicyDto;
import com.bookngo.bookingservice.dto.ShowDto;
import com.bookngo.bookingservice.dto.ShowPricingDto;
import com.bookngo.bookingservice.security.JwtService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ShowServiceClient {

    private final RestClient restClient;
    private final JwtService jwtService;
    private static final UUID SYSTEM_INTERNAL_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public ShowServiceClient(
            @Value("${services.show-service.url:http://localhost:8084}") String showServiceUrl,
            JwtService jwtService) {
        this.restClient = RestClient.builder()
                .baseUrl(showServiceUrl)
                .build();
        this.jwtService = jwtService;
    }

    public Optional<ShowDto> getShow(UUID showId) {
        if (showId == null) {
            return Optional.empty();
        }

        try {
            ShowDto show = restClient.get()
                    .uri("/api/v1/shows/{showId}", showId)
                    .retrieve()
                    .body(ShowDto.class);
            return Optional.ofNullable(show);
        } catch (Exception e) {
            log.warn("Failed to fetch show {}: {}", showId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<ShowPricingDto> getShowPricing(UUID showId) {
        if (showId == null) {
            return Collections.emptyList();
        }

        try {
            List<ShowPricingDto> pricing = restClient.get()
                    .uri("/api/v1/shows/{showId}/pricing", showId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ShowPricingDto>>() {});
            return pricing != null ? pricing : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch pricing for show {}: {}", showId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Optional<CancellationPolicyDto> getCancellationPolicy(UUID showId) {
        if (showId == null) {
            return Optional.empty();
        }

        try {
            String token = jwtService.generateToken(SYSTEM_INTERNAL_USER_ID, List.of("ADMIN"));
            CancellationPolicyDto policy = restClient.get()
                    .uri("/internal/v1/shows/{showId}/cancellation-policy", showId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(CancellationPolicyDto.class);
            return Optional.ofNullable(policy);
        } catch (Exception e) {
            log.warn("Failed to fetch cancellation policy for show {}: {}", showId, e.getMessage());
            return Optional.empty();
        }
    }
}
