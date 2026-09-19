package com.bookngo.userservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends ApiException {
    public InvalidOtpException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_OTP", message);
    }

    public InvalidOtpException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
