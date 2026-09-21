package com.bookngo.bookingservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.bookingservice.dto.SeatInventoryItem;
import com.bookngo.bookingservice.service.BookingService;

@RestController
@RequestMapping("/api/v1/shows")
public class SeatInventoryController {

    private final BookingService bookingService;

    public SeatInventoryController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 1. GET /api/v1/shows/{showId}/seat-inventory
     * Public endpoint returning real-time show seat availability.
     */
    @GetMapping("/{showId}/seat-inventory")
    public ResponseEntity<List<SeatInventoryItem>> getShowSeatInventory(@PathVariable UUID showId) {
        List<SeatInventoryItem> response = bookingService.getShowSeatInventory(showId);
        return ResponseEntity.ok(response);
    }
}
