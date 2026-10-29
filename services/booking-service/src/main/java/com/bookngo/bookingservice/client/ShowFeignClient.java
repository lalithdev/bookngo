package com.bookngo.bookingservice.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.bookngo.bookingservice.dto.CancellationPolicyDto;
import com.bookngo.bookingservice.dto.ShowDto;
import com.bookngo.bookingservice.dto.ShowPricingDto;

@FeignClient(name = "show-service")
public interface ShowFeignClient {

    @GetMapping("/api/v1/shows/{showId}")
    ShowDto getShow(@PathVariable("showId") UUID showId);

    @GetMapping("/api/v1/shows/{showId}/pricing")
    List<ShowPricingDto> getShowPricing(@PathVariable("showId") UUID showId);

    @GetMapping("/internal/v1/shows/{showId}/cancellation-policy")
    CancellationPolicyDto getCancellationPolicy(
            @RequestHeader("Authorization") String token,
            @PathVariable("showId") UUID showId);
}

