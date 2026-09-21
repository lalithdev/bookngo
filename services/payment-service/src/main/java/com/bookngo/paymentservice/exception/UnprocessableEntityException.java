package com.bookngo.paymentservice.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends ApiException {
    public UnprocessableEntityException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", message);
    }

    public UnprocessableEntityException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
