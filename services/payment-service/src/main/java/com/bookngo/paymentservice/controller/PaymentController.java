package com.bookngo.paymentservice.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.paymentservice.dto.PaymentInitiateRequest;
import com.bookngo.paymentservice.dto.PaymentReconciliationResponse;
import com.bookngo.paymentservice.dto.PaymentResponse;
import com.bookngo.paymentservice.dto.PaymentStatusResponse;
import com.bookngo.paymentservice.dto.ProviderCallbackRequest;
import com.bookngo.paymentservice.dto.RefundReversalResponse;
import com.bookngo.paymentservice.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/v1/payments
     * Initiates a payment. Amount and currency are fetched from Booking Service.
     * Requires JWT authentication and Idempotency-Key header.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentInitiateRequest request) {

        UUID userId = (UUID) authentication.getPrincipal();
        PaymentResponse response = paymentService.initiatePayment(userId, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/payments/{paymentId}/status
     * Returns current payment status. Only accessible by payment owner or ADMIN.
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable UUID paymentId,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);
        PaymentStatusResponse response = paymentService.getPaymentStatus(paymentId, userId, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/payments/provider/callback
     * Payment provider webhook. Authenticated by X-Provider-Signature (not JWT).
     * Callback processing is idempotent.
     */
    @PostMapping("/provider/callback")
    public ResponseEntity<Void> providerCallback(
            @Valid @RequestBody ProviderCallbackRequest callbackRequest) {

        paymentService.processProviderCallback(callbackRequest);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/v1/payments/{paymentId}/reconcile
     * Admin-only: reconcile UNKNOWN/TIMEOUT payments.
     */
    @PostMapping("/{paymentId}/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentReconciliationResponse> reconcilePayment(
            @PathVariable UUID paymentId,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);
        PaymentReconciliationResponse response = paymentService.reconcilePayment(paymentId, userId, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/payments/{paymentId}/refund
     * Returns refund/reversal status. Only accessible by payment owner or ADMIN.
     */
    @GetMapping("/{paymentId}/refund")
    public ResponseEntity<RefundReversalResponse> getRefundStatus(
            @PathVariable UUID paymentId,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);
        RefundReversalResponse response = paymentService.getRefundStatus(paymentId, userId, isAdmin);
        return ResponseEntity.ok(response);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
