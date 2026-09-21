package com.bookngo.paymentservice.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
