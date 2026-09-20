package com.bookngo.theatreservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.theatreservice.dto.ConfigureSeatsRequest;
import com.bookngo.theatreservice.dto.PhysicalSeatResponse;
import com.bookngo.theatreservice.service.TheatreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/screens")
public class ScreenController {

    private final TheatreService theatreService;

    public ScreenController(TheatreService theatreService) {
        this.theatreService = theatreService;
    }

    @GetMapping("/{screenId}/seats")
    public ResponseEntity<List<PhysicalSeatResponse>> getPhysicalSeatsByScreen(
            @PathVariable UUID screenId) {
        List<PhysicalSeatResponse> response = theatreService.getPhysicalSeatsByScreen(screenId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{screenId}/seats")
    @PreAuthorize("hasAnyRole('THEATRE_OPERATOR', 'ADMIN')")
    public ResponseEntity<List<PhysicalSeatResponse>> configurePhysicalSeats(
            @PathVariable UUID screenId,
            @Valid @RequestBody ConfigureSeatsRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isAdmin = checkIsAdmin(authentication);

        List<PhysicalSeatResponse> response = theatreService.configurePhysicalSeats(
                screenId, request, userId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID uuid) {
            return uuid;
        }
        return null;
    }

    private boolean checkIsAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
