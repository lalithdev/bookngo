package com.bookngo.bookingservice.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.bookngo.bookingservice.dto.RefundRequest;
import com.bookngo.bookingservice.dto.RefundResponse;
import com.bookngo.bookingservice.security.JwtService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaymentServiceClient {

    private final PaymentFeignClient paymentFeignClient;
    private final JwtService jwtService;
    private static final UUID SERVICE_CALLER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    public PaymentServiceClient(
            PaymentFeignClient paymentFeignClient,
            JwtService jwtService) {
        this.paymentFeignClient = paymentFeignClient;
        this.jwtService = jwtService;
    }

    public Optional<RefundResponse> initiateRefund(UUID paymentId, UUID bookingId, String reason) {
        if (paymentId == null) {
            return Optional.empty();
        }

        try {
            String token = jwtService.generateToken(SERVICE_CALLER_USER_ID, List.of("SERVICE"));
            RefundRequest request = RefundRequest.builder()
                    .bookingId(bookingId)
                    .reason(reason)
                    .build();

            RefundResponse response = paymentFeignClient.initiateRefund(
                    "Bearer " + token,
                    paymentId,
                    request);

            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.warn("Failed to initiate refund in Payment Service for payment {}: {}", paymentId, e.getMessage());
            return Optional.empty();
        }
    }
}
