package com.bookngo.bookingservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Retryer;

@Configuration
public class FeignConfig {

    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}

