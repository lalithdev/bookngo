package com.bookngo.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.userservice.dto.AuthTokenResponse;
import com.bookngo.userservice.dto.LoginRequest;
import com.bookngo.userservice.dto.OtpRequest;
import com.bookngo.userservice.dto.OtpRequestResponse;
import com.bookngo.userservice.dto.OtpVerifyRequest;
import com.bookngo.userservice.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/otp/request")
    public ResponseEntity<OtpRequestResponse> requestOtp(@Valid @RequestBody OtpRequest request) {
        OtpRequestResponse response = authService.requestOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthTokenResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        AuthTokenResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
