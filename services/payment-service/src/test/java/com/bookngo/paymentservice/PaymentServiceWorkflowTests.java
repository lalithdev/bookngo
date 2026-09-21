package com.bookngo.paymentservice;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.paymentservice.client.BookingServiceClient;
import com.bookngo.paymentservice.dto.ApplyPaymentPendingResponse;
import com.bookngo.paymentservice.entity.Payment;
import com.bookngo.paymentservice.entity.PaymentAttempt;
import com.bookngo.paymentservice.entity.PaymentAttemptStatus;
import com.bookngo.paymentservice.entity.PaymentStatus;
import com.bookngo.paymentservice.entity.RefundReversal;
import com.bookngo.paymentservice.entity.RefundReversalStatus;
import com.bookngo.paymentservice.repository.PaymentAttemptRepository;
import com.bookngo.paymentservice.repository.PaymentIdempotencyRecordRepository;
import com.bookngo.paymentservice.repository.PaymentReconciliationRepository;
import com.bookngo.paymentservice.repository.PaymentRepository;
import com.bookngo.paymentservice.repository.RefundReversalRepository;
import com.bookngo.paymentservice.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentServiceWorkflowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

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

    private UUID customerId;
    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        refundReversalRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        reconciliationRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        paymentRepository.deleteAll();

        customerId = UUID.randomUUID();
        customerToken = jwtService.generateToken(customerId, List.of("CUSTOMER"));
        adminToken = jwtService.generateToken(UUID.randomUUID(), List.of("ADMIN"));
    }

    @Test
    void initiatePayment_success() throws Exception {
        UUID bookingId = UUID.randomUUID();

        when(bookingServiceClient.applyPaymentPending(eq(bookingId), eq(customerId)))
                .thenReturn(ApplyPaymentPendingResponse.builder()
                        .bookingId(bookingId)
                        .totalAmount(new BigDecimal("250.00"))
                        .currency("USD")
                        .status("PAYMENT_PENDING")
                        .paymentGraceExpiresAt(OffsetDateTime.now().plusMinutes(2))
                        .build());

        String body = """
                {
                    "bookingId": "%s",
                    "providerName": "ASYNC"
                }
                """.formatted(bookingId);

        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "test-init-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID paymentId = UUID.fromString(json.get("paymentId").asText());

        Payment savedPayment = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
        assertEquals(new BigDecimal("250.00"), savedPayment.getAmount());
        assertEquals("USD", savedPayment.getCurrency());
        assertEquals(customerId, savedPayment.getUserId());

        List<PaymentAttempt> attempts = paymentAttemptRepository.findByPayment(savedPayment);
        assertEquals(1, attempts.size());
        assertEquals(PaymentAttemptStatus.INITIATED, attempts.get(0).getStatus());
    }

    @Test
    void initiatePayment_idempotent_duplicateKeyReturnsSameResult() throws Exception {
        UUID bookingId = UUID.randomUUID();

        when(bookingServiceClient.applyPaymentPending(eq(bookingId), eq(customerId)))
                .thenReturn(ApplyPaymentPendingResponse.builder()
                        .bookingId(bookingId)
                        .totalAmount(new BigDecimal("150.00"))
                        .currency("USD")
                        .status("PAYMENT_PENDING")
                        .build());

        String body = """
                {
                    "bookingId": "%s",
                    "providerName": "ASYNC"
                }
                """.formatted(bookingId);

        MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "same-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "same-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());

        assertEquals(firstJson.get("paymentId").asText(), secondJson.get("paymentId").asText());
        assertEquals(firstJson.get("status").asText(), secondJson.get("status").asText());
    }

    @Test
    void initiatePayment_idempotencyMismatch_returnsConflict() throws Exception {
        UUID bookingId1 = UUID.randomUUID();
        UUID bookingId2 = UUID.randomUUID();

        when(bookingServiceClient.applyPaymentPending(any(), any()))
                .thenReturn(ApplyPaymentPendingResponse.builder()
                        .bookingId(bookingId1)
                        .totalAmount(new BigDecimal("150.00"))
                        .currency("USD")
                        .status("PAYMENT_PENDING")
                        .build());

        String body1 = "{\"bookingId\": \"%s\", \"providerName\": \"STRIPE\"}".formatted(bookingId1);
        String body2 = "{\"bookingId\": \"%s\", \"providerName\": \"RAZORPAY\"}".formatted(bookingId2);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "mismatch-key-456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "mismatch-key-456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isConflict());
    }

    @Test
    void providerCallback_paymentSucceeded_transitionsStateAndNotifiesBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();

        when(bookingServiceClient.applyPaymentPending(eq(bookingId), eq(customerId)))
                .thenReturn(ApplyPaymentPendingResponse.builder()
                        .bookingId(bookingId)
                        .totalAmount(new BigDecimal("300.00"))
                        .currency("USD")
                        .status("PAYMENT_PENDING")
                        .build());

        // 1. Initiate with ASYNC provider so payment remains in PENDING
        String initBody = "{\"bookingId\": \"%s\", \"providerName\": \"ASYNC\"}".formatted(bookingId);
        MvcResult initResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "cb-test-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode initJson = objectMapper.readTree(initResult.getResponse().getContentAsString());
        UUID paymentId = UUID.fromString(initJson.get("paymentId").asText());

        // 2. Callback
        String callbackBody = """
                {
                    "paymentId": "%s",
                    "providerPaymentReference": "pi_stripe_12345",
                    "providerStatus": "SUCCESS"
                }
                """.formatted(paymentId);

        mockMvc.perform(post("/api/v1/payments/provider/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk());

        Payment updatedPayment = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, updatedPayment.getStatus());
        assertEquals("pi_stripe_12345", updatedPayment.getProviderPaymentReference());

        verify(bookingServiceClient, times(1)).notifyPaymentOutcome(bookingId, paymentId, "SUCCESS");
    }

    @Test
    void providerCallback_paymentFailed_transitionsStateAndNotifiesBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();

        when(bookingServiceClient.applyPaymentPending(eq(bookingId), eq(customerId)))
                .thenReturn(ApplyPaymentPendingResponse.builder()
                        .bookingId(bookingId)
                        .totalAmount(new BigDecimal("120.00"))
                        .currency("USD")
                        .status("PAYMENT_PENDING")
                        .build());

        // 1. Initiate with ASYNC provider so payment remains in PENDING
        String initBody = "{\"bookingId\": \"%s\", \"providerName\": \"ASYNC\"}".formatted(bookingId);
        MvcResult initResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "cb-fail-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode initJson = objectMapper.readTree(initResult.getResponse().getContentAsString());
        UUID paymentId = UUID.fromString(initJson.get("paymentId").asText());

        // 2. Callback failure
        String callbackBody = """
                {
                    "paymentId": "%s",
                    "providerPaymentReference": "pi_failed_123",
                    "providerStatus": "FAILURE"
                }
                """.formatted(paymentId);

        mockMvc.perform(post("/api/v1/payments/provider/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk());

        Payment updatedPayment = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.FAILURE, updatedPayment.getStatus());

        verify(bookingServiceClient, times(1)).notifyPaymentOutcome(bookingId, paymentId, "FAILURE");
    }

    @Test
    void internalRefund_success() throws Exception {
        // Create an existing SUCCESS payment
        Payment payment = Payment.builder()
                .bookingId(UUID.randomUUID())
                .userId(customerId)
                .amount(new BigDecimal("180.00"))
                .currency("USD")
                .status(PaymentStatus.SUCCESS)
                .providerName("STRIPE")
                .providerPaymentReference("pi_success_refund_me")
                .build();
        payment = paymentRepository.save(payment);

        PaymentAttempt attempt = PaymentAttempt.builder()
                .payment(payment)
                .providerAttemptReference("att_success_1")
                .status(PaymentAttemptStatus.SUCCESS)
                .build();
        paymentAttemptRepository.save(attempt);

        String refundBody = """
                {
                    "bookingId": "%s",
                    "reason": "Customer cancellation within policy"
                }
                """.formatted(payment.getBookingId());

        mockMvc.perform(post("/internal/v1/payments/" + payment.getPaymentId() + "/refund")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getPaymentId().toString()))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.amount").value(180.00));

        Payment updated = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, updated.getStatus());

        List<RefundReversal> refunds = refundReversalRepository.findByPayment(payment);
        assertEquals(1, refunds.size());
        assertEquals(RefundReversalStatus.REFUNDED, refunds.get(0).getStatus());
        assertEquals(new BigDecimal("180.00"), refunds.get(0).getAmount());
    }

    @Test
    void reconcile_success() throws Exception {
        Payment payment = Payment.builder()
                .bookingId(UUID.randomUUID())
                .userId(customerId)
                .amount(new BigDecimal("210.00"))
                .currency("USD")
                .status(PaymentStatus.UNKNOWN)
                .providerName("STRIPE")
                .providerPaymentReference("pi_unknown_1")
                .build();
        payment = paymentRepository.save(payment);

        PaymentAttempt attempt = PaymentAttempt.builder()
                .payment(payment)
                .providerAttemptReference("att_init_1")
                .status(PaymentAttemptStatus.UNKNOWN)
                .build();
        paymentAttemptRepository.save(attempt);

        mockMvc.perform(post("/api/v1/payments/" + payment.getPaymentId() + "/reconcile")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getPaymentId().toString()))
                .andExpect(jsonPath("$.status").isNotEmpty());

        assertEquals(1, reconciliationRepository.count());
    }
}
