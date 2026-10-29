package com.bookngo.userservice.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.bookngo.userservice.entity.OtpVerification;
import com.bookngo.userservice.repository.OtpVerificationRepository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class OtpServiceLoggingSecurityTest {

    private OtpVerificationRepository otpRepository;
    private OtpService otpService;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger otpLogger;

    @BeforeEach
    void setUp() {
        otpRepository = mock(OtpVerificationRepository.class);
        otpService = new OtpService(otpRepository, new BCryptPasswordEncoder(), true, 300, 3, 5, 300);

        otpLogger = (Logger) LoggerFactory.getLogger(OtpService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        otpLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (otpLogger != null && listAppender != null) {
            otpLogger.detachAppender(listAppender);
        }
    }

    @Test
    void testRequestOtp_RevealsPlaintextOtpCodeInSimulationLog() {
        String phone = "+919876543210";
        when(otpRepository.countByPhoneNumberAndCreatedAtAfter(eq(phone), any())).thenReturn(0L);
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> {
            OtpVerification entity = invocation.getArgument(0);
            entity.setOtpId(java.util.UUID.randomUUID());
            return entity;
        });

        OtpVerification result = otpService.requestOtp(phone);
        assertNotNull(result);

        assertFalse(listAppender.list.isEmpty(), "At least one log event should be emitted for simulation");

        String expectedOtp = otpService.getLastGeneratedOtp();
        assertNotNull(expectedOtp);

        boolean found = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("[OTP SIMULATION] Delivered OTP code [" + expectedOtp + "]"));
        assertTrue(found, "Log message must reveal the generated plaintext OTP code for simulation");
    }
}

