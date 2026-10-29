package com.bookngo.bookingservice.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.bookngo.bookingservice.dto.PhysicalSeatDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TheatreServiceClient {

    private final TheatreFeignClient theatreFeignClient;

    public TheatreServiceClient(TheatreFeignClient theatreFeignClient) {
        this.theatreFeignClient = theatreFeignClient;
    }

    public List<PhysicalSeatDto> getPhysicalSeatsByScreen(UUID screenId) {
        if (screenId == null) {
            return Collections.emptyList();
        }

        try {
            List<PhysicalSeatDto> seats = theatreFeignClient.getPhysicalSeatsByScreen(screenId);
            return seats != null ? seats : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch physical seats for screen {}: {}", screenId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
