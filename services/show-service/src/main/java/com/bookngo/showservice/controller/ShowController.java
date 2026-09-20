package com.bookngo.showservice.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.bookngo.showservice.dto.CancellationPolicyResponse;
import com.bookngo.showservice.dto.CancellationPolicyUpdateRequest;
import com.bookngo.showservice.dto.ConfigurePricingRequest;
import com.bookngo.showservice.dto.ShowCreateRequest;
import com.bookngo.showservice.dto.ShowPricingResponse;
import com.bookngo.showservice.dto.ShowResponse;
import com.bookngo.showservice.service.ShowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping
    public ResponseEntity<List<ShowResponse>> searchShows(
            @RequestParam(required = false) UUID movieId,
            @RequestParam(required = false) UUID theatreId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String format) {

        List<ShowResponse> shows = showService.searchShows(movieId, theatreId, city, date, language, format);
        return ResponseEntity.ok(shows);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('THEATRE_OPERATOR', 'ADMIN')")
    public ResponseEntity<ShowResponse> createShow(
            @Valid @RequestBody ShowCreateRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = checkIsAdmin(authentication);

        ShowResponse response = showService.createShow(request, userId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{showId}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable UUID showId) {
        ShowResponse response = showService.getShowById(showId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{showId}/pricing")
    public ResponseEntity<List<ShowPricingResponse>> getShowPricing(@PathVariable UUID showId) {
        List<ShowPricingResponse> response = showService.getShowPricing(showId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{showId}/pricing")
    @PreAuthorize("hasAnyRole('THEATRE_OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ShowPricingResponse>> configureShowPricing(
            @PathVariable UUID showId,
            @Valid @RequestBody ConfigurePricingRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = checkIsAdmin(authentication);

        List<ShowPricingResponse> response = showService.configureShowPricing(showId, request, userId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{showId}/cancellation-policy")
    public ResponseEntity<CancellationPolicyResponse> getCancellationPolicy(@PathVariable UUID showId) {
        CancellationPolicyResponse response = showService.getCancellationPolicy(showId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{showId}/cancellation-policy")
    @PreAuthorize("hasAnyRole('THEATRE_OPERATOR', 'ADMIN')")
    public ResponseEntity<CancellationPolicyResponse> configureCancellationPolicy(
            @PathVariable UUID showId,
            @Valid @RequestBody CancellationPolicyUpdateRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = checkIsAdmin(authentication);

        CancellationPolicyResponse response = showService.configureCancellationPolicy(showId, request, userId, isAdmin);
        return ResponseEntity.ok(response);
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
