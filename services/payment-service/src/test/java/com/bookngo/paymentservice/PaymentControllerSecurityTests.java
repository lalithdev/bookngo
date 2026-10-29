package com.bookngo.paymentservice;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.paymentservice.client.BookingServiceClient;
import com.bookngo.paymentservice.dto.ApplyPaymentPendingResponse;
import com.bookngo.paymentservice.entity.Payment;
import com.bookngo.paymentservice.entity.PaymentStatus;
import com.bookngo.paymentservice.repository.PaymentAttemptRepository;
import com.bookngo.paymentservice.repository.PaymentIdempotencyRecordRepository;
import com.bookngo.paymentservice.repository.PaymentReconciliationRepository;
import com.bookngo.paymentservice.repository.PaymentRepository;
import com.bookngo.paymentservice.repository.RefundReversalRepository;
import com.bookngo.paymentservice.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private RefundReversalRepository refundReversalRepository;

    @Autowired
    private PaymentReconciliationRepository reconciliationRepository;

    @Autowired
    private PaymentIdempotencyRecordRepository idempotencyRecordRepository;

    @MockitoBean
    private BookingServiceClient bookingServiceClient;

    private UUID userId1;
    private UUID userId2;
    private String userToken1;
    private String userToken2;
    private String adminToken;

    @BeforeEach
    void setUp() {
        refundReversalRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        reconciliationRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        paymentRepository.deleteAll();

        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();

        userToken1 = jwtService.generateToken(userId1, List.of("CUSTOMER"));
        userToken2 = jwtService.generateToken(userId2, List.of("CUSTOMER"));
        adminToken = jwtService.generateToken(UUID.randomUUID(), List.of("ADMIN"));
    }

    @Test
    void unauthenticatedUser_cannotInitiatePayment() throws Exception {
        UUID bookingId = UUID.randomUUID();
        String body = """
                {
                    "bookingId": "%s",
                    "providerName": "STRIPE"
                }
                """.formatted(bookingId);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idemp-sec-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedUser_cannotGetPaymentStatus() throws Exception {
        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID() + "/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customer_cannotAccessInternalRefundEndpoint() throws Exception {
        UUID bookingId = UUID.randomUUID();
        String body = """
                {
                    "bookingId": "%s",
                    "reason": "Customer requested refund"
                }
                """.formatted(bookingId);

        mockMvc.perform(post("/internal/v1/payments/" + UUID.randomUUID() + "/refund")
                        .header("Authorization", "Bearer " + userToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void customer_cannotAccessAnotherUsersPaymentStatus() throws Exception {
        // Create payment belonging to userId1
        Payment payment = Payment.builder()
                .bookingId(UUID.randomUUID())
                .userId(userId1)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(PaymentStatus.INITIATED)
                .build();
        payment = paymentRepository.save(payment);

        // User2 attempts to access User1's payment status
        mockMvc.perform(get("/api/v1/payments/" + payment.getPaymentId() + "/status")
                        .header("Authorization", "Bearer " + userToken2))
                .andExpect(status().isForbidden());
    }

    @Test
    void owner_canAccessOwnPaymentStatus() throws Exception {
        Payment payment = Payment.builder()
                .bookingId(UUID.randomUUID())
                .userId(userId1)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(PaymentStatus.INITIATED)
                .build();
        payment = paymentRepository.save(payment);

        mockMvc.perform(get("/api/v1/payments/" + payment.getPaymentId() + "/status")
                        .header("Authorization", "Bearer " + userToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getPaymentId().toString()))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    void admin_canAccessAnyPaymentStatus() throws Exception {
        Payment payment = Payment.builder()
                .bookingId(UUID.randomUUID())
                .userId(userId1)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(PaymentStatus.INITIATED)
                .build();
        payment = paymentRepository.save(payment);

        mockMvc.perform(get("/api/v1/payments/" + payment.getPaymentId() + "/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getPaymentId().toString()));
    }

    @Test
    void unauthenticatedInternalRefund_returns401() throws Exception {
        mockMvc.perform(post("/internal/v1/payments/" + UUID.randomUUID() + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"" + UUID.randomUUID() + "\",\"reason\":\"CANCELLED\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void customer_cannotAccessInternalRefund_returns403() throws Exception {
        mockMvc.perform(post("/internal/v1/payments/" + UUID.randomUUID() + "/refund")
                        .header("Authorization", "Bearer " + userToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"" + UUID.randomUUID() + "\",\"reason\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void serviceToken_isAuthorizedOnInternalRefund() throws Exception {
        String serviceToken = jwtService.generateToken(UUID.fromString("00000000-0000-0000-0000-000000000005"), List.of("SERVICE"));
        // Returns 404 (payment not found) or domain error, proving auth check passed (not 401 or 403)
        mockMvc.perform(post("/internal/v1/payments/" + UUID.randomUUID() + "/refund")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"" + UUID.randomUUID() + "\",\"reason\":\"CANCELLED\"}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, s, "Must not be 401");
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, s, "Must not be 403");
                });
    }

    @Test
    void malformedAndExpiredTokens_return401() throws Exception {
        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID() + "/status")
                        .header("Authorization", "Bearer malformed.jwt.token"))
                .andExpect(status().isUnauthorized());

        JwtService expiredJwt = new JwtService("test-secret-key-for-payment-service-tests-at-least-32-chars", -5000);
        String expired = expiredJwt.generateToken(UUID.randomUUID(), List.of("CUSTOMER"));

        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID() + "/status")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtSecretValidation_throwsOnInvalidSecret() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> new JwtService(null, 3600000));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> new JwtService("", 3600000));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> new JwtService("   ", 3600000));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> new JwtService("short-key", 3600000));
    }
}
