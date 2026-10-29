package com.bookngo.showservice.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "movie-service")
public interface MovieFeignClient {

    @GetMapping("/api/v1/movies")
    List<Map<String, Object>> getMovies(@RequestParam(value = "language", required = false) String language);
}

