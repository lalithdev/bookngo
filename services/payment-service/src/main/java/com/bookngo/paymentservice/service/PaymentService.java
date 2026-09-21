package com.bookngo.paymentservice.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookngo.paymentservice.client.BookingServiceClient;
import com.bookngo.paymentservice.dto.ApplyPaymentPendingResponse;
import com.bookngo.paymentservice.dto.PaymentInitiateRequest;
import com.bookngo.paymentservice.dto.PaymentReconciliationResponse;
import com.bookngo.paymentservice.dto.PaymentResponse;
import com.bookngo.paymentservice.dto.PaymentStatusResponse;
import com.bookngo.paymentservice.dto.ProviderCallbackRequest;
import com.bookngo.paymentservice.dto.RefundReversalResponse;
import com.bookngo.paymentservice.entity.IdempotencyStatus;
import com.bookngo.paymentservice.entity.Payment;
import com.bookngo.paymentservice.entity.PaymentAttempt;
import com.bookngo.paymentservice.entity.PaymentAttemptStatus;
import com.bookngo.paymentservice.entity.PaymentIdempotencyRecord;
import com.bookngo.paymentservice.entity.PaymentReconciliation;
import com.bookngo.paymentservice.entity.PaymentStatus;
import com.bookngo.paymentservice.entity.ReconciliationStatus;
import com.bookngo.paymentservice.entity.RefundReversal;
import com.bookngo.paymentservice.entity.RefundReversalStatus;
import com.bookngo.paymentservice.exception.BadRequestException;
import com.bookngo.paymentservice.exception.ConflictException;
import com.bookngo.paymentservice.exception.ForbiddenException;
import com.bookngo.paymentservice.exception.ResourceNotFoundException;
import com.bookngo.paymentservice.exception.UnprocessableEntityException;
import com.bookngo.paymentservice.repository.PaymentAttemptRepository;
import com.bookngo.paymentservice.repository.PaymentIdempotencyRecordRepository;
import com.bookngo.paymentservice.repository.PaymentReconciliationRepository;
import com.bookngo.paymentservice.repository.PaymentRepository;
import com.bookngo.paymentservice.repository.RefundReversalRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RefundReversalRepository refundReversalRepository;
    private final PaymentReconciliationRepository reconciliationRepository;
    private final PaymentIdempotencyRecordRepository idempotencyRecordRepository;
    private final BookingServiceClient bookingServiceClient;
    private final PaymentSimulatorService simulatorService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            RefundReversalRepository refundReversalRepository,
            PaymentReconciliationRepository reconciliationRepository,
            PaymentIdempotencyRecordRepository idempotencyRecordRepository,
            BookingServiceClient bookingServiceClient,
            PaymentSimulatorService simulatorService) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.refundReversalRepository = refundReversalRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.bookingServiceClient = bookingServiceClient;
        this.simulatorService = simulatorService;
    }

    // -------------------------------------------------------------------------
    // 1. POST /api/v1/payments — Initiate Payment
    // -------------------------------------------------------------------------

    @Transactional
    public PaymentResponse initiatePayment(UUID userId, String idempotencyKey, PaymentInitiateRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }

        UUID bookingId = request.getBookingId();
        String providerName = request.getProviderName();

        // --- Idempotency Check ---
        String requestHash = computeSha256(bookingId + ":" + providerName);

        Optional<PaymentIdempotencyRecord> existingRecordOpt =
                idempotencyRecordRepository.findByUserIdAndOperationTypeAndIdempotencyKey(
                        userId, "INITIATE_PAYMENT", idempotencyKey);

        if (existingRecordOpt.isPresent()) {
            PaymentIdempotencyRecord existing = existingRecordOpt.get();
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new ConflictException("IDEMPOTENCY_MISMATCH",
                        "Idempotency key reused with a different request payload");
            }
            if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new ConflictException("OPERATION_IN_PROGRESS",
                        "Payment initiation already in progress for this idempotency key");
            }
            if (existing.getStatus() == IdempotencyStatus.COMPLETED && existing.getPayment() != null) {
                return mapToPaymentResponse(existing.getPayment(), null);
            }
        }

        // --- Create idempotency record as IN_PROGRESS ---
        PaymentIdempotencyRecord idempotencyRecord = existingRecordOpt.orElseGet(() ->
                PaymentIdempotencyRecord.builder()
                        .userId(userId)
                        .operationType("INITIATE_PAYMENT")
                        .idempotencyKey(idempotencyKey)
                        .build()
        );
        idempotencyRecord.setRequestHash(requestHash);
        idempotencyRecord.setStatus(IdempotencyStatus.IN_PROGRESS);
        idempotencyRecordRepository.save(idempotencyRecord);

        try {
            // --- Call Booking Service to get authoritative amount/currency ---
            // This also transitions the booking to PAYMENT_PENDING with 2-min grace.
            ApplyPaymentPendingResponse bookingResponse =
                    bookingServiceClient.applyPaymentPending(bookingId, userId);

            BigDecimal authorizedAmount = bookingResponse.getTotalAmount();
            String currency = bookingResponse.getCurrency();
            OffsetDateTime graceExpiresAt = bookingResponse.getPaymentGraceExpiresAt();

            // --- Create Payment record (INITIATED → PENDING) ---
            Payment payment = Payment.builder()
                    .bookingId(bookingId)
                    .userId(userId)
                    .amount(authorizedAmount)
                    .currency(currency)
                    .status(PaymentStatus.PENDING)
                    .providerName(providerName)
                    .build();
            payment = paymentRepository.save(payment);

            // --- Create PaymentAttempt (INITIATED) ---
            PaymentAttempt attempt = PaymentAttempt.builder()
                    .payment(payment)
                    .status(PaymentAttemptStatus.INITIATED)
                    .build();
            attempt = paymentAttemptRepository.save(attempt);

            // --- Execute simulation or note for async callback ---
            String outcome;
            String providerRef;

            if (simulatorService.isSimulationEnabled()) {
                providerRef = simulatorService.simulateProviderReference(providerName);
                outcome = simulatorService.simulateOutcome(providerName);
            } else {
                // Real provider integration point (not implemented in Review-1)
                throw new UnprocessableEntityException("Real payment provider integration not configured");
            }

            // Update attempt with provider reference
            attempt.setProviderAttemptReference(providerRef);

            if ("PENDING".equalsIgnoreCase(outcome)) {
                // Asynchronous provider flow: payment stays in PENDING waiting for webhook/callback
                attempt.setStatus(PaymentAttemptStatus.INITIATED);
                attempt = paymentAttemptRepository.save(attempt);

                payment.setProviderPaymentReference(providerRef);
                payment = paymentRepository.save(payment);

                idempotencyRecord.setPayment(payment);
                idempotencyRecord.setPaymentAttempt(attempt);
                idempotencyRecord.setStatus(IdempotencyStatus.COMPLETED);
                idempotencyRecordRepository.save(idempotencyRecord);

                return mapToPaymentResponse(payment, graceExpiresAt);
            }

            attempt.setStatus(toAttemptStatus(outcome));
            attempt.setCompletedAt(OffsetDateTime.now());
            attempt = paymentAttemptRepository.save(attempt);

            // Update payment with provider reference
            payment.setProviderPaymentReference(providerRef);

            // --- Mark idempotency as COMPLETED ---
            idempotencyRecord.setPayment(payment);
            idempotencyRecord.setPaymentAttempt(attempt);
            idempotencyRecord.setStatus(IdempotencyStatus.COMPLETED);
            idempotencyRecordRepository.save(idempotencyRecord);

            // --- Process the outcome through the full state machine ---
            // This applies the real state transitions including Booking Service notification.
            payment = applyOutcomeToPayment(payment, attempt, outcome);

            return mapToPaymentResponse(payment, graceExpiresAt);

        } catch (Exception e) {
            // Roll back idempotency record to FAILED so a retry is not blocked
            idempotencyRecord.setStatus(IdempotencyStatus.FAILED);
            idempotencyRecordRepository.save(idempotencyRecord);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // 2. GET /api/v1/payments/{paymentId}/status
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID paymentId, UUID requestingUserId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (!isAdmin && !payment.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("You are not authorized to view this payment");
        }

        return PaymentStatusResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .status(payment.getStatus())
                .providerPaymentReference(payment.getProviderPaymentReference())
                .build();
    }

    // -------------------------------------------------------------------------
    // 3. POST /api/v1/payments/provider/callback
    // -------------------------------------------------------------------------

    @Transactional
    public void processProviderCallback(ProviderCallbackRequest callbackRequest) {
        UUID paymentId = callbackRequest.getPaymentId();
        String providerRef = callbackRequest.getProviderPaymentReference();
        String providerStatus = callbackRequest.getProviderStatus();

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        // Idempotent: if already terminal, acknowledge without re-processing
        if (isTerminalStatus(payment.getStatus())) {
            log.info("Callback received for already-terminal payment {} (status={}). Acknowledging idempotently.",
                    paymentId, payment.getStatus());
            return;
        }

        // Deduplicate by providerPaymentReference if already set
        if (payment.getProviderPaymentReference() != null
                && payment.getProviderPaymentReference().equals(providerRef)
                && isTerminalStatus(payment.getStatus())) {
            log.info("Duplicate callback with same providerRef {} for payment {}. Idempotent acknowledge.", providerRef, paymentId);
            return;
        }

        // Resolve latest attempt
        PaymentAttempt attempt = paymentAttemptRepository
                .findFirstByPaymentOrderByInitiatedAtDesc(payment)
                .orElse(null);

        if (attempt != null) {
            attempt.setStatus(toAttemptStatus(providerStatus));
            attempt.setProviderAttemptReference(providerRef);
            attempt.setCompletedAt(OffsetDateTime.now());
            paymentAttemptRepository.save(attempt);
        }

        payment.setProviderPaymentReference(providerRef);
        applyOutcomeToPayment(payment, attempt, providerStatus);
    }

    // -------------------------------------------------------------------------
    // 4. POST /api/v1/payments/{paymentId}/reconcile (ADMIN only)
    // -------------------------------------------------------------------------

    @Transactional
    public PaymentReconciliationResponse reconcilePayment(UUID paymentId, UUID requestingUserId, boolean isAdmin) {
        if (!isAdmin) {
            throw new ForbiddenException("Only admins can initiate payment reconciliation");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.UNKNOWN && payment.getStatus() != PaymentStatus.TIMEOUT) {
            throw new ConflictException("Payment is not in UNKNOWN or TIMEOUT state; cannot reconcile: " + payment.getStatus());
        }

        // Check provider outcome via simulator (or real provider in production)
        String resolvedOutcome;
        String providerStatusObserved;
        ReconciliationStatus reconciliationStatus;

        if (simulatorService.isSimulationEnabled()) {
            resolvedOutcome = simulatorService.simulateReconciliationOutcome(
                    payment.getProviderName(), payment.getProviderPaymentReference());
            providerStatusObserved = resolvedOutcome;
            reconciliationStatus = ReconciliationStatus.RESOLVED;
        } else {
            throw new UnprocessableEntityException("Real provider reconciliation not configured");
        }

        // Record the reconciliation attempt
        PaymentReconciliation rec = PaymentReconciliation.builder()
                .payment(payment)
                .status(reconciliationStatus)
                .providerStatusObserved(providerStatusObserved)
                .checkedAt(OffsetDateTime.now())
                .build();
        rec = reconciliationRepository.save(rec);

        // Apply the resolved outcome through the real state machine
        PaymentAttempt attempt = paymentAttemptRepository
                .findFirstByPaymentOrderByInitiatedAtDesc(payment)
                .orElse(null);

        PaymentStatus resolvedPaymentStatus;
        if ("RESOLVED".equals(reconciliationStatus.name())) {
            payment = applyOutcomeToPayment(payment, attempt, resolvedOutcome);
            resolvedPaymentStatus = payment.getStatus();
        } else {
            resolvedPaymentStatus = payment.getStatus();
        }

        return PaymentReconciliationResponse.builder()
                .reconciliationId(rec.getReconciliationId())
                .paymentId(payment.getPaymentId())
                .status(reconciliationStatus)
                .providerStatusObserved(providerStatusObserved)
                .resolvedPaymentStatus(resolvedPaymentStatus)
                .build();
    }

    // -------------------------------------------------------------------------
    // 5. POST /internal/v1/payments/{paymentId}/refund — Internal (called by Booking Service)
    // -------------------------------------------------------------------------

    @Transactional
    public RefundReversalResponse initiateRefund(UUID paymentId, UUID bookingId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        // Only SUCCESS or REFUND_PENDING payments can be refunded
        if (payment.getStatus() != PaymentStatus.SUCCESS
                && payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new ConflictException("Payment cannot be refunded in status: " + payment.getStatus());
        }

        // Transition payment to REFUND_PENDING
        payment.setStatus(PaymentStatus.REFUND_PENDING);
        payment = paymentRepository.save(payment);

        // Resolve the latest successful attempt for reference
        PaymentAttempt attempt = paymentAttemptRepository
                .findFirstByPaymentOrderByInitiatedAtDesc(payment)
                .orElse(null);

        // Create RefundReversal record (INITIATED → REFUND_PENDING → REFUNDED in simulation)
        RefundReversal reversal = RefundReversal.builder()
                .payment(payment)
                .paymentAttempt(attempt)
                .amount(payment.getAmount())
                .status(RefundReversalStatus.REFUND_PENDING)
                .build();
        reversal = refundReversalRepository.save(reversal);

        // In simulation mode: immediately resolve refund
        if (simulatorService.isSimulationEnabled()) {
            String simulatedRefundRef = "SIM-REFUND-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            reversal.setStatus(RefundReversalStatus.REFUNDED);
            reversal.setProviderReference(simulatedRefundRef);
            reversal.setCompletedAt(OffsetDateTime.now());
            reversal = refundReversalRepository.save(reversal);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment = paymentRepository.save(payment);
        }

        return RefundReversalResponse.builder()
                .refundReversalId(reversal.getRefundReversalId())
                .paymentId(payment.getPaymentId())
                .amount(reversal.getAmount())
                .status(reversal.getStatus())
                .providerReference(reversal.getProviderReference())
                .build();
    }

    // -------------------------------------------------------------------------
    // 6. GET /api/v1/payments/{paymentId}/refund
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public RefundReversalResponse getRefundStatus(UUID paymentId, UUID requestingUserId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (!isAdmin && !payment.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("You are not authorized to view this refund");
        }

        List<RefundReversal> reversals = refundReversalRepository
                .findByPaymentOrderByCreatedAtDesc(payment);

        if (reversals.isEmpty()) {
            throw new ResourceNotFoundException("No refund found for payment: " + paymentId);
        }

        RefundReversal latest = reversals.get(0);

        return RefundReversalResponse.builder()
                .refundReversalId(latest.getRefundReversalId())
                .paymentId(payment.getPaymentId())
                .amount(latest.getAmount())
                .status(latest.getStatus())
                .providerReference(latest.getProviderReference())
                .build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies a payment outcome through the real state machine and notifies Booking Service.
     * This is the single path for all outcome processing (simulation, callback, reconciliation).
     */
    private Payment applyOutcomeToPayment(Payment payment, PaymentAttempt attempt, String outcome) {
        String normalizedOutcome = outcome != null ? outcome.toUpperCase() : "";

        switch (normalizedOutcome) {
            case "SUCCESS" -> {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment = paymentRepository.save(payment);
                bookingServiceClient.notifyPaymentOutcome(payment.getBookingId(), payment.getPaymentId(), "SUCCESS");
            }
            case "FAILURE" -> {
                payment.setStatus(PaymentStatus.FAILURE);
                payment = paymentRepository.save(payment);
                bookingServiceClient.notifyPaymentOutcome(payment.getBookingId(), payment.getPaymentId(), "FAILURE");
            }
            case "CANCELLED" -> {
                payment.setStatus(PaymentStatus.CANCELLED);
                payment = paymentRepository.save(payment);
                bookingServiceClient.notifyPaymentOutcome(payment.getBookingId(), payment.getPaymentId(), "CANCELLED");
            }
            case "UNKNOWN" -> {
                // Do NOT notify Booking Service yet; booking enters PAYMENT_UNKNOWN, awaiting reconciliation.
                payment.setStatus(PaymentStatus.UNKNOWN);
                payment = paymentRepository.save(payment);
                bookingServiceClient.notifyPaymentOutcome(payment.getBookingId(), payment.getPaymentId(), "UNKNOWN");
            }
            default -> {
                log.warn("Unrecognised payment outcome '{}' for payment {}. Treating as UNKNOWN.", outcome, payment.getPaymentId());
                payment.setStatus(PaymentStatus.UNKNOWN);
                payment = paymentRepository.save(payment);
                bookingServiceClient.notifyPaymentOutcome(payment.getBookingId(), payment.getPaymentId(), "UNKNOWN");
            }
        }

        return payment;
    }

    private boolean isTerminalStatus(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS
                || status == PaymentStatus.FAILURE
                || status == PaymentStatus.CANCELLED
                || status == PaymentStatus.REFUND_PENDING
                || status == PaymentStatus.REFUNDED;
    }

    private PaymentAttemptStatus toAttemptStatus(String outcome) {
        if (outcome == null) return PaymentAttemptStatus.UNKNOWN;
        return switch (outcome.toUpperCase()) {
            case "SUCCESS" -> PaymentAttemptStatus.SUCCESS;
            case "FAILURE" -> PaymentAttemptStatus.FAILURE;
            case "CANCELLED" -> PaymentAttemptStatus.CANCELLED;
            case "TIMEOUT" -> PaymentAttemptStatus.TIMEOUT;
            default -> PaymentAttemptStatus.UNKNOWN;
        };
    }

    private PaymentResponse mapToPaymentResponse(Payment payment, OffsetDateTime paymentGraceExpiresAt) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBookingId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .providerPaymentReference(payment.getProviderPaymentReference())
                .paymentGraceExpiresAt(paymentGraceExpiresAt)
                .build();
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
