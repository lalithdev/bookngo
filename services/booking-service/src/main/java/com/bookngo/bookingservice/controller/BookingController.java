package com.bookngo.bookingservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.bookingservice.dto.BookingResponse;
import com.bookngo.bookingservice.dto.TicketResponse;
import com.bookngo.bookingservice.service.BookingService;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 5. GET /api/v1/bookings/me
     * Returns booking history for the logged-in customer.
     */
    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<BookingResponse> response = bookingService.getMyBookings(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 6. GET /api/v1/bookings/{bookingId}
     * Returns booking details for owner, operator, or admin.
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isPrivileged = checkIsPrivileged(authentication);
        BookingResponse response = bookingService.getBookingById(bookingId, userId, isPrivileged);
        return ResponseEntity.ok(response);
    }

    /**
     * 7. POST /api/v1/bookings/{bookingId}/cancel
     * Cancels confirmed booking if eligible.
     */
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isPrivileged = checkIsPrivileged(authentication);
        BookingResponse response = bookingService.cancelBooking(bookingId, userId, isPrivileged);
        return ResponseEntity.ok(response);
    }

    /**
     * 8. GET /api/v1/bookings/{bookingId}/ticket
     * Returns ticket for confirmed booking.
     */
    @GetMapping("/{bookingId}/ticket")
    public ResponseEntity<TicketResponse> getTicketByBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isPrivileged = checkIsPrivileged(authentication);
        TicketResponse response = bookingService.getTicketByBooking(bookingId, userId, isPrivileged);
        return ResponseEntity.ok(response);
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
