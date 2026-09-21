package com.bookngo.paymentservice.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.paymentservice.dto.InternalRefundRequest;
import com.bookngo.paymentservice.dto.RefundReversalResponse;
import com.bookngo.paymentservice.service.PaymentService;

import jakarta.validation.Valid;

/**
 * Internal controller for service-to-service communication.
 * These endpoints are NOT routed through the API Gateway.
 * Protected by JWT authentication (service-to-service token with ADMIN role).
 */
@RestController
@RequestMapping("/internal/v1/payments")
@PreAuthorize("hasRole('ADMIN')")
public class InternalPaymentController {

    private final PaymentService paymentService;

    public InternalPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /internal/v1/payments/{paymentId}/refund
     * Called by Booking Service when a confirmed booking is cancelled and needs a refund.
     * Authentication: Internal JWT (SYSTEM_INTERNAL_USER_ID + ADMIN role).
     * NOT exposed through API Gateway.
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<RefundReversalResponse> initiateRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody InternalRefundRequest request) {

        RefundReversalResponse response = paymentService.initiateRefund(
                paymentId, request.getBookingId(), request.getReason());
        return ResponseEntity.ok(response);
    }
}
