package com.bookngo.paymentservice.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.bookngo.paymentservice.dto.ApplyPaymentPendingRequest;
import com.bookngo.paymentservice.dto.ApplyPaymentPendingResponse;
import com.bookngo.paymentservice.dto.PaymentOutcomeRequest;

@FeignClient(name = "booking-service")
public interface BookingFeignClient {

    @PostMapping("/internal/v1/bookings/{bookingId}/apply-payment-pending")
    ApplyPaymentPendingResponse applyPaymentPending(
            @RequestHeader("Authorization") String token,
            @PathVariable("bookingId") UUID bookingId,
            @RequestBody ApplyPaymentPendingRequest request);

    @PostMapping("/internal/v1/bookings/{bookingId}/payment-outcome")
    void notifyPaymentOutcome(
            @RequestHeader("Authorization") String token,
            @PathVariable("bookingId") UUID bookingId,
            @RequestBody PaymentOutcomeRequest request);
}

