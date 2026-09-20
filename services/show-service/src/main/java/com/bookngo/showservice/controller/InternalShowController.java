package com.bookngo.showservice.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.showservice.dto.CancellationPolicyResponse;
import com.bookngo.showservice.service.ShowService;

@RestController
@RequestMapping("/internal/v1/shows")
public class InternalShowController {

    private final ShowService showService;

    public InternalShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping("/{showId}/cancellation-policy")
    public ResponseEntity<CancellationPolicyResponse> internalGetCancellationPolicy(
            @PathVariable UUID showId) {
        CancellationPolicyResponse response = showService.getCancellationPolicy(showId);
        return ResponseEntity.ok(response);
    }
}
