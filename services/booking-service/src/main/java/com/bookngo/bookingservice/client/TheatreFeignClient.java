package com.bookngo.bookingservice.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.bookngo.bookingservice.dto.PhysicalSeatDto;

@FeignClient(name = "theatre-service")
public interface TheatreFeignClient {

    @GetMapping("/api/v1/screens/{screenId}/seats")
    List<PhysicalSeatDto> getPhysicalSeatsByScreen(@PathVariable("screenId") UUID screenId);
}

