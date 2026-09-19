package com.bookngo.userservice.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookngo.userservice.entity.OtpVerification;
import com.bookngo.userservice.exception.InvalidOtpException;
import com.bookngo.userservice.exception.RateLimitExceededException;
import com.bookngo.userservice.repository.OtpVerificationRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private final long expirationSeconds;
    private final int maxAttempts;
    private final int rateLimitPerWindow;
    private final long rateLimitWindowSeconds;
    private final boolean simulationEnabled;

    // In-memory or simulated OTP holder for test validation / inspection
    private String lastGeneratedOtp;

    public OtpService(
            OtpVerificationRepository otpRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.otp.simulation-enabled:${otp.simulation-enabled:true}}") boolean simulationEnabled,
            @Value("${otp.expiration-seconds:300}") long expirationSeconds,
            @Value("${otp.max-attempts:3}") int maxAttempts,
            @Value("${otp.rate-limit-per-window:5}") int rateLimitPerWindow,
            @Value("${otp.rate-limit-window-seconds:300}") long rateLimitWindowSeconds) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.simulationEnabled = simulationEnabled;
        this.expirationSeconds = expirationSeconds;
        this.maxAttempts = maxAttempts;
        this.rateLimitPerWindow = rateLimitPerWindow;
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }

    @Transactional
    public OtpVerification requestOtp(String phoneNumber) {
        OffsetDateTime windowStart = OffsetDateTime.now().minusSeconds(rateLimitWindowSeconds);
        long recentRequests = otpRepository.countByPhoneNumberAndCreatedAtAfter(phoneNumber, windowStart);
        if (recentRequests >= rateLimitPerWindow) {
            throw new RateLimitExceededException("Too many OTP requests for this phone number. Please wait before retrying.");
        }

        // Generate 6-digit cryptographic OTP
        int randomCode = secureRandom.nextInt(900000) + 100000;
        String otpCode = String.valueOf(randomCode);
        this.lastGeneratedOtp = otpCode;

        // Securely hash OTP secret before persisting
        String otpSecretHash = passwordEncoder.encode(otpCode);

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(expirationSeconds);

        OtpVerification verification = OtpVerification.builder()
                .phoneNumber(phoneNumber)
                .otpSecretHash(otpSecretHash)
                .status(OtpVerification.STATUS_PENDING)
                .createdAt(now)
                .expiresAt(expiresAt)
                .attemptCount(0)
                .rateLimitMetadata("{\"window_requests\":" + (recentRequests + 1) + "}")
                .build();

        OtpVerification saved = otpRepository.save(verification);

        // Simulation delivery log for Review 1
        log.info("[OTP SIMULATION] Delivered OTP code [{}] to phone [{}] (expires in {}s)", otpCode, phoneNumber, expirationSeconds);

        return saved;
    }

    @Transactional
    public OtpVerification verifyOtp(String phoneNumber, String otpCode) {
        Optional<OtpVerification> recordOpt = otpRepository.findFirstByPhoneNumberOrderByCreatedAtDesc(phoneNumber);

        if (recordOpt.isEmpty()) {
            throw new InvalidOtpException("No OTP request found for this phone number");
        }

        OtpVerification record = recordOpt.get();

        if (OtpVerification.STATUS_VERIFIED.equals(record.getStatus())) {
            throw new InvalidOtpException("This OTP has already been verified");
        }

        if (OtpVerification.STATUS_LOCKED.equals(record.getStatus())) {
            throw new InvalidOtpException("Verification locked due to excessive failed attempts. Please request a new OTP.");
        }

        if (OtpVerification.STATUS_EXPIRED.equals(record.getStatus()) || OffsetDateTime.now().isAfter(record.getExpiresAt())) {
            record.setStatus(OtpVerification.STATUS_EXPIRED);
            otpRepository.save(record);
            throw new InvalidOtpException("OTP has expired. Please request a new one.");
        }

        if (record.getAttemptCount() >= maxAttempts) {
            record.setStatus(OtpVerification.STATUS_LOCKED);
            otpRepository.save(record);
            throw new InvalidOtpException("Verification locked due to excessive failed attempts. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(otpCode, record.getOtpSecretHash())) {
            int newAttemptCount = record.getAttemptCount() + 1;
            record.setAttemptCount(newAttemptCount);
            if (newAttemptCount >= maxAttempts) {
                record.setStatus(OtpVerification.STATUS_LOCKED);
            }
            otpRepository.save(record);
            throw new InvalidOtpException("Invalid OTP code provided");
        }

        // Verification successful
        record.setStatus(OtpVerification.STATUS_VERIFIED);
        record.setVerifiedAt(OffsetDateTime.now());
        return otpRepository.save(record);
    }

    public String getLastGeneratedOtp() {
        return lastGeneratedOtp;
    }
}
