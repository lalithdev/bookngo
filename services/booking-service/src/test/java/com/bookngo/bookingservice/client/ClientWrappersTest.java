package com.bookngo.bookingservice.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bookngo.bookingservice.dto.CancellationPolicyDto;
import com.bookngo.bookingservice.dto.PhysicalSeatDto;
import com.bookngo.bookingservice.dto.RefundRequest;
import com.bookngo.bookingservice.dto.RefundResponse;
import com.bookngo.bookingservice.dto.ShowDto;
import com.bookngo.bookingservice.dto.ShowPricingDto;
import com.bookngo.bookingservice.security.JwtService;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
class ClientWrappersTest {

    @Mock
    private ShowFeignClient showFeignClient;

    @Mock
    private TheatreFeignClient theatreFeignClient;

    @Mock
    private PaymentFeignClient paymentFeignClient;

    @Mock
    private JwtService jwtService;

    private ShowServiceClient showServiceClient;
    private TheatreServiceClient theatreServiceClient;
    private PaymentServiceClient paymentServiceClient;

    private final UUID showId = UUID.randomUUID();
    private final UUID screenId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        showServiceClient = new ShowServiceClient(showFeignClient, jwtService);
        theatreServiceClient = new TheatreServiceClient(theatreFeignClient);
        paymentServiceClient = new PaymentServiceClient(paymentFeignClient, jwtService);
    }

    @Test
    @DisplayName("ShowServiceClient.getShow returns ShowDto on success")
    void getShow_success() {
        ShowDto mockShow = ShowDto.builder()
                .showId(showId)
                .screenId(screenId)
                .movieId(UUID.randomUUID())
                .startsAt(OffsetDateTime.now())
                .endsAt(OffsetDateTime.now().plusHours(2))
                .status("SCHEDULED")
                .build();
        when(showFeignClient.getShow(showId)).thenReturn(mockShow);

        Optional<ShowDto> result = showServiceClient.getShow(showId);

        assertTrue(result.isPresent());
        assertEquals(showId, result.get().getShowId());
    }

    @Test
    @DisplayName("ShowServiceClient.getShow returns Optional.empty on Feign failure")
    void getShow_failure() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/shows/" + showId,
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        when(showFeignClient.getShow(showId)).thenThrow(new FeignException.NotFound("Not Found", request, null, null));

        Optional<ShowDto> result = showServiceClient.getShow(showId);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("ShowServiceClient.getShowPricing returns pricing list on success")
    void getShowPricing_success() {
        ShowPricingDto pricingDto = ShowPricingDto.builder()
                .seatCategory("PLATINUM")
                .amount(new BigDecimal("250.00"))
                .currency("INR")
                .build();
        when(showFeignClient.getShowPricing(showId)).thenReturn(List.of(pricingDto));

        List<ShowPricingDto> result = showServiceClient.getShowPricing(showId);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("250.00"), result.get(0).getAmount());
    }

    @Test
    @DisplayName("ShowServiceClient.getShowPricing returns empty list on Feign failure")
    void getShowPricing_failure() {
        when(showFeignClient.getShowPricing(showId)).thenThrow(new RuntimeException("Connection timed out"));

        List<ShowPricingDto> result = showServiceClient.getShowPricing(showId);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("ShowServiceClient.getCancellationPolicy propagates Bearer token")
    void getCancellationPolicy_success() {
        when(jwtService.generateToken(any(), any())).thenReturn("mock-jwt-token");
        CancellationPolicyDto mockPolicy = CancellationPolicyDto.builder()
                .policyType("WINDOWED")
                .status("ACTIVE")
                .build();
        when(showFeignClient.getCancellationPolicy(eq("Bearer mock-jwt-token"), eq(showId)))
                .thenReturn(mockPolicy);

        Optional<CancellationPolicyDto> result = showServiceClient.getCancellationPolicy(showId);

        assertTrue(result.isPresent());
        assertEquals("WINDOWED", result.get().getPolicyType());
        verify(showFeignClient).getCancellationPolicy("Bearer mock-jwt-token", showId);
    }

    @Test
    @DisplayName("TheatreServiceClient.getPhysicalSeatsByScreen returns seats on success")
    void getPhysicalSeatsByScreen_success() {
        PhysicalSeatDto seatDto = PhysicalSeatDto.builder()
                .physicalSeatId(UUID.randomUUID())
                .rowLabel("A")
                .seatNumber("1")
                .seatCategory("STANDARD")
                .build();
        when(theatreFeignClient.getPhysicalSeatsByScreen(screenId)).thenReturn(List.of(seatDto));

        List<PhysicalSeatDto> result = theatreServiceClient.getPhysicalSeatsByScreen(screenId);

        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getRowLabel());
    }

    @Test
    @DisplayName("PaymentServiceClient.initiateRefund propagates Bearer token and refund request")
    void initiateRefund_success() {
        when(jwtService.generateToken(any(), any())).thenReturn("mock-admin-token");
        RefundResponse mockResponse = RefundResponse.builder()
                .refundReversalId(UUID.randomUUID())
                .status("REFUNDED")
                .amount(new BigDecimal("180.00"))
                .build();
        when(paymentFeignClient.initiateRefund(eq("Bearer mock-admin-token"), eq(paymentId), any(RefundRequest.class)))
                .thenReturn(mockResponse);

        Optional<RefundResponse> result = paymentServiceClient.initiateRefund(paymentId, bookingId, "Cancellation");

        assertTrue(result.isPresent());
        assertEquals("REFUNDED", result.get().getStatus());
        verify(paymentFeignClient).initiateRefund(startsWith("Bearer "), eq(paymentId), any(RefundRequest.class));
    }
}
