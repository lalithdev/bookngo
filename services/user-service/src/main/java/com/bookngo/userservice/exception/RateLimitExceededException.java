package com.bookngo.userservice.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ApiException {
    public RateLimitExceededException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", message);
    }
}
