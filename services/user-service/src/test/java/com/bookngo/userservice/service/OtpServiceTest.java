package com.bookngo.userservice.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bookngo.userservice.entity.OtpVerification;
import com.bookngo.userservice.exception.InvalidOtpException;
import com.bookngo.userservice.exception.RateLimitExceededException;
import com.bookngo.userservice.repository.OtpVerificationRepository;

class OtpServiceTest {

    private OtpVerificationRepository otpRepository;
    private PasswordEncoder passwordEncoder;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpRepository = Mockito.mock(OtpVerificationRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        otpService = new OtpService(otpRepository, passwordEncoder, true, 300, 3, 5, 300);
    }

    @Test
    void testRequestOtp_Success() {
        String phone = "+919876543210";
        when(otpRepository.countByPhoneNumberAndCreatedAtAfter(eq(phone), any())).thenReturn(0L);
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpVerification result = otpService.requestOtp(phone);

        assertNotNull(result);
        assertEquals(phone, result.getPhoneNumber());
        assertEquals(OtpVerification.STATUS_PENDING, result.getStatus());
        assertNotNull(result.getOtpSecretHash());
        assertNotNull(result.getExpiresAt());
        assertTrue(result.getExpiresAt().isAfter(result.getCreatedAt()));
    }

    @Test
    void testRequestOtp_RateLimitExceeded() {
        String phone = "+919876543210";
        when(otpRepository.countByPhoneNumberAndCreatedAtAfter(eq(phone), any())).thenReturn(5L);

        assertThrows(RateLimitExceededException.class, () -> otpService.requestOtp(phone));
    }

    @Test
    void testVerifyOtp_Success() {
        String phone = "+919876543210";
        String plainCode = "123456";
        String hashedCode = passwordEncoder.encode(plainCode);

        OtpVerification verification = OtpVerification.builder()
                .otpId(UUID.randomUUID())
                .phoneNumber(phone)
                .otpSecretHash(hashedCode)
                .status(OtpVerification.STATUS_PENDING)
                .createdAt(OffsetDateTime.now().minusMinutes(1))
                .expiresAt(OffsetDateTime.now().plusMinutes(4))
                .attemptCount(0)
                .build();

        when(otpRepository.findFirstByPhoneNumberOrderByCreatedAtDesc(phone)).thenReturn(Optional.of(verification));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpVerification result = otpService.verifyOtp(phone, plainCode);

        assertEquals(OtpVerification.STATUS_VERIFIED, result.getStatus());
        assertNotNull(result.getVerifiedAt());
    }

    @Test
    void testVerifyOtp_WrongCode_IncrementsAttempts() {
        String phone = "+919876543210";
        String hashedCode = passwordEncoder.encode("123456");

        OtpVerification verification = OtpVerification.builder()
                .otpId(UUID.randomUUID())
                .phoneNumber(phone)
                .otpSecretHash(hashedCode)
                .status(OtpVerification.STATUS_PENDING)
                .createdAt(OffsetDateTime.now().minusMinutes(1))
                .expiresAt(OffsetDateTime.now().plusMinutes(4))
                .attemptCount(0)
                .build();

        when(otpRepository.findFirstByPhoneNumberOrderByCreatedAtDesc(phone)).thenReturn(Optional.of(verification));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(InvalidOtpException.class, () -> otpService.verifyOtp(phone, "999999"));
        assertEquals(1, verification.getAttemptCount());
    }

    @Test
    void testVerifyOtp_MaxAttemptsExceeded_Locks() {
        String phone = "+919876543210";
        String hashedCode = passwordEncoder.encode("123456");

        OtpVerification verification = OtpVerification.builder()
                .otpId(UUID.randomUUID())
                .phoneNumber(phone)
                .otpSecretHash(hashedCode)
                .status(OtpVerification.STATUS_PENDING)
                .createdAt(OffsetDateTime.now().minusMinutes(1))
                .expiresAt(OffsetDateTime.now().plusMinutes(4))
                .attemptCount(2)
                .build();

        when(otpRepository.findFirstByPhoneNumberOrderByCreatedAtDesc(phone)).thenReturn(Optional.of(verification));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(InvalidOtpException.class, () -> otpService.verifyOtp(phone, "999999"));
        assertEquals(3, verification.getAttemptCount());
        assertEquals(OtpVerification.STATUS_LOCKED, verification.getStatus());
    }

    @Test
    void testVerifyOtp_Expired() {
        String phone = "+919876543210";
        String hashedCode = passwordEncoder.encode("123456");

        OtpVerification verification = OtpVerification.builder()
                .otpId(UUID.randomUUID())
                .phoneNumber(phone)
                .otpSecretHash(hashedCode)
                .status(OtpVerification.STATUS_PENDING)
                .createdAt(OffsetDateTime.now().minusMinutes(6))
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .attemptCount(0)
                .build();

        when(otpRepository.findFirstByPhoneNumberOrderByCreatedAtDesc(phone)).thenReturn(Optional.of(verification));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(InvalidOtpException.class, () -> otpService.verifyOtp(phone, "123456"));
        assertEquals(OtpVerification.STATUS_EXPIRED, verification.getStatus());
    }
}
