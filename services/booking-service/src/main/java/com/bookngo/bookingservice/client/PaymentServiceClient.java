package com.bookngo.bookingservice.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bookngo.bookingservice.dto.RefundRequest;
import com.bookngo.bookingservice.dto.RefundResponse;
import com.bookngo.bookingservice.security.JwtService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaymentServiceClient {

    private final RestClient restClient;
    private final JwtService jwtService;
    private static final UUID SYSTEM_INTERNAL_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public PaymentServiceClient(
            @Value("${services.payment-service.url:http://localhost:8086}") String paymentServiceUrl,
            JwtService jwtService) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
        this.jwtService = jwtService;
    }

    public Optional<RefundResponse> initiateRefund(UUID paymentId, UUID bookingId, String reason) {
        if (paymentId == null) {
            return Optional.empty();
        }

        try {
            String token = jwtService.generateToken(SYSTEM_INTERNAL_USER_ID, List.of("ADMIN"));
            RefundRequest request = RefundRequest.builder()
                    .bookingId(bookingId)
                    .reason(reason)
                    .build();

            RefundResponse response = restClient.post()
                    .uri("/internal/v1/payments/{paymentId}/refund", paymentId)
                    .header("Authorization", "Bearer " + token)
                    .body(request)
                    .retrieve()
                    .body(RefundResponse.class);

            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.warn("Failed to initiate refund in Payment Service for payment {}: {}", paymentId, e.getMessage());
            return Optional.empty();
        }
    }
}
