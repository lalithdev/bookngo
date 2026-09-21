package com.bookngo.paymentservice.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.bookngo.paymentservice.dto.ApplyPaymentPendingRequest;
import com.bookngo.paymentservice.dto.ApplyPaymentPendingResponse;
import com.bookngo.paymentservice.dto.PaymentOutcomeRequest;
import com.bookngo.paymentservice.exception.ConflictException;
import com.bookngo.paymentservice.exception.ResourceNotFoundException;
import com.bookngo.paymentservice.exception.UnprocessableEntityException;
import com.bookngo.paymentservice.security.JwtService;

import lombok.extern.slf4j.Slf4j;

/**
 * Internal client for Booking Service.
 * Reuses the exact same internal auth pattern as Booking Service's PaymentServiceClient:
 * generate an internal JWT token using SYSTEM_INTERNAL_USER_ID with ADMIN role.
 *
 * This client calls:
 *   POST /internal/v1/bookings/{bookingId}/apply-payment-pending
 *   POST /internal/v1/bookings/{bookingId}/payment-outcome
 *
 * These endpoints are NOT exposed through the API Gateway and are protected
 * by the same JWT filter that guards all authenticated Booking Service endpoints.
 */
@Slf4j
@Component
public class BookingServiceClient {

    /**
     * The same system internal user ID as used in Booking Service's PaymentServiceClient.
     * This UUID is the agreed-upon sentinel for internal service-to-service calls.
     */
    private static final UUID SYSTEM_INTERNAL_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final RestClient restClient;
    private final JwtService jwtService;

    public BookingServiceClient(
            @Value("${services.booking-service.url:http://localhost:8085}") String bookingServiceUrl,
            JwtService jwtService) {
        this.restClient = RestClient.builder()
                .baseUrl(bookingServiceUrl)
                .build();
        this.jwtService = jwtService;
    }

    /**
     * Calls POST /internal/v1/bookings/{bookingId}/apply-payment-pending.
     * Sets PAYMENT_PENDING on the booking and returns authoritative amount/currency.
     *
     * @param bookingId the booking to lock for payment
     * @param userId    the user initiating payment (validated against booking owner in Booking Service)
     * @return authoritative booking details including amount and currency
     */
    public ApplyPaymentPendingResponse applyPaymentPending(UUID bookingId, UUID userId) {
        String token = jwtService.generateToken(SYSTEM_INTERNAL_USER_ID, List.of("ADMIN"));
        ApplyPaymentPendingRequest request = ApplyPaymentPendingRequest.builder()
                .userId(userId)
                .build();
        try {
            ApplyPaymentPendingResponse response = restClient.post()
                    .uri("/internal/v1/bookings/{bookingId}/apply-payment-pending", bookingId)
                    .header("Authorization", "Bearer " + token)
                    .body(request)
                    .retrieve()
                    .body(ApplyPaymentPendingResponse.class);
            return response;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictException("Booking is not in a payable state: " + bookingId);
        } catch (HttpClientErrorException e) {
            throw new UnprocessableEntityException("Booking Service rejected payment pending request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to call applyPaymentPending on Booking Service for bookingId {}: {}", bookingId, e.getMessage());
            throw new UnprocessableEntityException("Unable to reach Booking Service: " + e.getMessage());
        }
    }

    /**
     * Calls POST /internal/v1/bookings/{bookingId}/payment-outcome.
     * Notifies Booking Service of the payment result so it can transition booking state.
     *
     * @param bookingId booking to update
     * @param paymentId the payment record that was processed
     * @param outcome   SUCCESS | FAILURE | CANCELLED | UNKNOWN
     */
    public void notifyPaymentOutcome(UUID bookingId, UUID paymentId, String outcome) {
        String token = jwtService.generateToken(SYSTEM_INTERNAL_USER_ID, List.of("ADMIN"));
        PaymentOutcomeRequest request = PaymentOutcomeRequest.builder()
                .paymentId(paymentId)
                .outcome(outcome)
                .build();
        try {
            restClient.post()
                    .uri("/internal/v1/bookings/{bookingId}/payment-outcome", bookingId)
                    .header("Authorization", "Bearer " + token)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Log but do not rethrow: payment state is already committed, Booking Service
            // will reconcile via its own mechanisms or admin intervention.
            log.error("Failed to notify Booking Service of payment outcome for bookingId {}, paymentId {}, outcome {}: {}",
                    bookingId, paymentId, outcome, e.getMessage());
        }
    }
}
