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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.theatreservice.dto.ScreenCreateRequest;
import com.bookngo.theatreservice.dto.ScreenResponse;
import com.bookngo.theatreservice.dto.TheatreCreateRequest;
import com.bookngo.theatreservice.dto.TheatreResponse;
import com.bookngo.theatreservice.service.TheatreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/theatres")
public class TheatreController {

    private final TheatreService theatreService;

    public TheatreController(TheatreService theatreService) {
        this.theatreService = theatreService;
    }

    @GetMapping
    public ResponseEntity<List<TheatreResponse>> listTheatres(
            @RequestParam(required = false) String city) {
        List<TheatreResponse> response = theatreService.listTheatres(city);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('THEATRE_OPERATOR', 'ADMIN')")
    public ResponseEntity<TheatreResponse> createTheatre(
            @Valid @RequestBody TheatreCreateRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isAdmin = checkIsAdmin(authentication);

        TheatreResponse response = theatreService.createTheatre(request, userId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{theatreId}")
    public ResponseEntity<TheatreResponse> getTheatreById(@PathVariable UUID theatreId) {
        TheatreResponse response = theatreService.getTheatreById(theatreId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{theatreId}")
    @PreAuthorize("hasAnyRole('THEATRE_OPERATOR', 'ADMIN')")
    public ResponseEntity<TheatreResponse> updateTheatre(
            @PathVariable UUID theatreId,
            @Valid @RequestBody TheatreCreateRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isAdmin = checkIsAdmin(authentication);

        TheatreResponse response = theatreService.updateTheatre(theatreId, request, userId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{theatreId}/screens")
    public ResponseEntity<List<ScreenResponse>> getScreensByTheatre(@PathVariable UUID theatreId) {
        List<ScreenResponse> response = theatreService.getScreensByTheatre(theatreId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{theatreId}/screens")
    @PreAuthorize("hasAnyRole('THEATRE_OPERATOR', 'ADMIN')")
    public ResponseEntity<ScreenResponse> createScreen(
            @PathVariable UUID theatreId,
            @Valid @RequestBody ScreenCreateRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isAdmin = checkIsAdmin(authentication);

        ScreenResponse response = theatreService.createScreen(theatreId, request, userId, isAdmin);
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
