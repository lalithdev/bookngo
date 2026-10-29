package com.bookngo.paymentservice.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bookngo.paymentservice.dto.ApplyPaymentPendingRequest;
import com.bookngo.paymentservice.dto.ApplyPaymentPendingResponse;
import com.bookngo.paymentservice.dto.PaymentOutcomeRequest;
import com.bookngo.paymentservice.exception.ConflictException;
import com.bookngo.paymentservice.exception.ResourceNotFoundException;
import com.bookngo.paymentservice.exception.UnprocessableEntityException;
import com.bookngo.paymentservice.security.JwtService;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
class BookingServiceClientTest {

    @Mock
    private BookingFeignClient bookingFeignClient;

    @Mock
    private JwtService jwtService;

    private BookingServiceClient bookingServiceClient;

    private final UUID bookingId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final Request dummyRequest = Request.create(
            Request.HttpMethod.POST,
            "/internal/v1/bookings/" + bookingId + "/apply-payment-pending",
            Collections.emptyMap(),
            null,
            new RequestTemplate());

    @BeforeEach
    void setUp() {
        bookingServiceClient = new BookingServiceClient(bookingFeignClient, jwtService);
    }

    @Test
    @DisplayName("applyPaymentPending succeeds and propagates Bearer token")
    void applyPaymentPending_success() {
        when(jwtService.generateToken(any(), any())).thenReturn("admin-token");
        ApplyPaymentPendingResponse mockResponse = ApplyPaymentPendingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("250.00"))
                .currency("INR")
                .build();

        when(bookingFeignClient.applyPaymentPending(eq("Bearer admin-token"), eq(bookingId), any(ApplyPaymentPendingRequest.class)))
                .thenReturn(mockResponse);

        ApplyPaymentPendingResponse response = bookingServiceClient.applyPaymentPending(bookingId, userId);

        assertNotNull(response);
        assertEquals(bookingId, response.getBookingId());
        assertEquals(new BigDecimal("250.00"), response.getTotalAmount());
        verify(bookingFeignClient).applyPaymentPending("Bearer admin-token", bookingId,
                ApplyPaymentPendingRequest.builder().userId(userId).build());
    }

    @Test
    @DisplayName("applyPaymentPending maps Feign 404 to ResourceNotFoundException")
    void applyPaymentPending_404_throwsResourceNotFoundException() {
        when(jwtService.generateToken(any(), any())).thenReturn("admin-token");
        when(bookingFeignClient.applyPaymentPending(any(), any(), any()))
                .thenThrow(new FeignException.NotFound("Not Found", dummyRequest, null, null));

        assertThrows(ResourceNotFoundException.class, () ->
                bookingServiceClient.applyPaymentPending(bookingId, userId));
    }

    @Test
    @DisplayName("applyPaymentPending maps Feign 409 to ConflictException")
    void applyPaymentPending_409_throwsConflictException() {
        when(jwtService.generateToken(any(), any())).thenReturn("admin-token");
        when(bookingFeignClient.applyPaymentPending(any(), any(), any()))
                .thenThrow(new FeignException.Conflict("Conflict", dummyRequest, null, null));

        assertThrows(ConflictException.class, () ->
                bookingServiceClient.applyPaymentPending(bookingId, userId));
    }

    @Test
    @DisplayName("applyPaymentPending maps Feign other 4xx to UnprocessableEntityException")
    void applyPaymentPending_4xx_throwsUnprocessableEntityException() {
        when(jwtService.generateToken(any(), any())).thenReturn("admin-token");
        when(bookingFeignClient.applyPaymentPending(any(), any(), any()))
                .thenThrow(new FeignException.BadRequest("Bad Request", dummyRequest, null, null));

        assertThrows(UnprocessableEntityException.class, () ->
                bookingServiceClient.applyPaymentPending(bookingId, userId));
    }

    @Test
    @DisplayName("applyPaymentPending maps connection / 5xx error to UnprocessableEntityException")
    void applyPaymentPending_5xx_throwsUnprocessableEntityException() {
        when(jwtService.generateToken(any(), any())).thenReturn("admin-token");
        when(bookingFeignClient.applyPaymentPending(any(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThrows(UnprocessableEntityException.class, () ->
                bookingServiceClient.applyPaymentPending(bookingId, userId));
    }

    @Test
    @DisplayName("notifyPaymentOutcome succeeds and propagates token")
    void notifyPaymentOutcome_success() {
        when(jwtService.generateToken(any(), any())).thenReturn("admin-token");
        doNothing().when(bookingFeignClient).notifyPaymentOutcome(eq("Bearer admin-token"), eq(bookingId), any(PaymentOutcomeRequest.class));

        bookingServiceClient.notifyPaymentOutcome(bookingId, paymentId, "SUCCESS");

        verify(bookingFeignClient).notifyPaymentOutcome("Bearer admin-token", bookingId,
                PaymentOutcomeRequest.builder().paymentId(paymentId).outcome("SUCCESS").build());
    }

    @Test
    @DisplayName("notifyPaymentOutcome logs and suppresses downstream exception")
    void notifyPaymentOutcome_handlesErrorGracefully() {
        when(jwtService.generateToken(any(), any())).thenReturn("admin-token");
        doThrow(new RuntimeException("Network down")).when(bookingFeignClient)
                .notifyPaymentOutcome(any(), any(), any());

        // Should not throw
        bookingServiceClient.notifyPaymentOutcome(bookingId, paymentId, "SUCCESS");
    }
}

