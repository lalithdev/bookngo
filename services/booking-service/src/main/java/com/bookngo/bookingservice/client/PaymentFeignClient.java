package com.bookngo.bookingservice.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.bookngo.bookingservice.dto.RefundRequest;
import com.bookngo.bookingservice.dto.RefundResponse;

@FeignClient(name = "payment-service")
public interface PaymentFeignClient {

    @PostMapping("/internal/v1/payments/{paymentId}/refund")
    RefundResponse initiateRefund(
            @RequestHeader("Authorization") String token,
            @PathVariable("paymentId") UUID paymentId,
            @RequestBody RefundRequest request);
}

