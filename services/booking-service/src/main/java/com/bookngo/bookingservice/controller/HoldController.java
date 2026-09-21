package com.bookngo.bookingservice.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.bookingservice.dto.SeatHoldDto;
import com.bookngo.bookingservice.dto.SeatHoldRequest;
import com.bookngo.bookingservice.dto.SeatHoldResponse;
import com.bookngo.bookingservice.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class HoldController {

    private final BookingService bookingService;

    public HoldController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 2. POST /api/v1/shows/{showId}/holds
     * Creates hold and initiates booking.
     */
    @PostMapping("/shows/{showId}/holds")
    public ResponseEntity<SeatHoldResponse> createSeatHold(
            @PathVariable UUID showId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SeatHoldRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        SeatHoldResponse response = bookingService.createSeatHold(showId, userId, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 3. GET /api/v1/holds/{holdId}
     * Returns hold status.
     */
    @GetMapping("/holds/{holdId}")
    public ResponseEntity<SeatHoldDto> getHoldById(
            @PathVariable UUID holdId,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isPrivileged = checkIsPrivileged(authentication);
        SeatHoldDto response = bookingService.getHoldById(holdId, userId, isPrivileged);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. DELETE /api/v1/holds/{holdId}
     * Releases unpaid hold.
     */
    @DeleteMapping("/holds/{holdId}")
    public ResponseEntity<Void> releaseHold(
            @PathVariable UUID holdId,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isPrivileged = checkIsPrivileged(authentication);
        bookingService.releaseHold(holdId, userId, isPrivileged);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID uuid) {
            return uuid;
        }
        return null;
    }

    private boolean checkIsPrivileged(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_THEATRE_OPERATOR".equals(a.getAuthority()));
    }
}
