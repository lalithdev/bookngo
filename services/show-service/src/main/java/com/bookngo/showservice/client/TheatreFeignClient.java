package com.bookngo.showservice.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "theatre-service")
public interface TheatreFeignClient {

    @GetMapping("/api/v1/theatres")
    List<Map<String, Object>> getTheatres(@RequestParam(value = "city", required = false) String city);
}

