package com.bookngo.bookingservice.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.bookingservice.dto.ApplyPaymentPendingInternalResponse;
import com.bookngo.bookingservice.dto.ApplyPaymentPendingRequest;
import com.bookngo.bookingservice.dto.PaymentOutcomeInternalResponse;
import com.bookngo.bookingservice.dto.PaymentOutcomeRequest;
import com.bookngo.bookingservice.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/v1/bookings")
public class InternalBookingController {

    private final BookingService bookingService;

    public InternalBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 9. POST /internal/v1/bookings/{bookingId}/apply-payment-pending
     * Called by Payment Service to apply 2-minute payment grace period.
     */
    @PostMapping("/{bookingId}/apply-payment-pending")
    public ResponseEntity<ApplyPaymentPendingInternalResponse> applyPaymentPending(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ApplyPaymentPendingRequest request) {
        ApplyPaymentPendingInternalResponse response = bookingService.applyPaymentPending(bookingId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 10. POST /internal/v1/bookings/{bookingId}/payment-outcome
     * Called by Payment Service to process final payment outcome.
     */
    @PostMapping("/{bookingId}/payment-outcome")
    public ResponseEntity<PaymentOutcomeInternalResponse> processPaymentOutcome(
            @PathVariable UUID bookingId,
            @Valid @RequestBody PaymentOutcomeRequest request) {
        PaymentOutcomeInternalResponse response = bookingService.processPaymentOutcome(bookingId, request);
        return ResponseEntity.ok(response);
    }
}
