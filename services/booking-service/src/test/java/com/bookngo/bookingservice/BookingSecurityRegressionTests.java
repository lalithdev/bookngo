package com.bookngo.bookingservice;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.bookingservice.client.PaymentServiceClient;
import com.bookngo.bookingservice.client.ShowServiceClient;
import com.bookngo.bookingservice.client.TheatreServiceClient;
import com.bookngo.bookingservice.dto.ApplyPaymentPendingRequest;
import com.bookngo.bookingservice.dto.PaymentOutcomeRequest;
import com.bookngo.bookingservice.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingSecurityRegressionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ShowServiceClient showServiceClient;

    @MockitoBean
    private TheatreServiceClient theatreServiceClient;

    @MockitoBean
    private PaymentServiceClient paymentServiceClient;

    private UUID customerId;
    private String customerToken;
    private String serviceToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customerToken = jwtService.generateToken(customerId, List.of("CUSTOMER"));
        serviceToken = jwtService.generateToken(UUID.fromString("00000000-0000-0000-0000-000000000006"), List.of("SERVICE"));
        adminToken = jwtService.generateToken(UUID.randomUUID(), List.of("ADMIN"));
    }

    @Test
    @DisplayName("Regression: Unauthenticated calls to Booking internal endpoints return 401 Unauthorized")
    void testUnauthenticatedInternalEndpointsFailWith401() throws Exception {
        UUID bookingId = UUID.randomUUID();
        ApplyPaymentPendingRequest req = ApplyPaymentPendingRequest.builder().userId(customerId).build();

        mockMvc.perform(post("/internal/v1/bookings/" + bookingId + "/apply-payment-pending")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        PaymentOutcomeRequest outcomeReq = PaymentOutcomeRequest.builder()
                .paymentId(UUID.randomUUID())
                .outcome("SUCCESS")
                .build();

        mockMvc.perform(post("/internal/v1/bookings/" + bookingId + "/payment-outcome")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outcomeReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Regression: Customer token attempting to call Booking internal endpoints is rejected with 403 Forbidden")
    void testCustomerTokenInternalEndpointsFailWith403() throws Exception {
        UUID bookingId = UUID.randomUUID();
        ApplyPaymentPendingRequest req = ApplyPaymentPendingRequest.builder().userId(customerId).build();

        mockMvc.perform(post("/internal/v1/bookings/" + bookingId + "/apply-payment-pending")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        PaymentOutcomeRequest outcomeReq = PaymentOutcomeRequest.builder()
                .paymentId(UUID.randomUUID())
                .outcome("SUCCESS")
                .build();

        mockMvc.perform(post("/internal/v1/bookings/" + bookingId + "/payment-outcome")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outcomeReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Regression: Service token is authorized on Booking internal endpoints")
    void testServiceTokenInternalEndpointsAllowed() throws Exception {
        UUID bookingId = UUID.randomUUID();
        ApplyPaymentPendingRequest req = ApplyPaymentPendingRequest.builder().userId(customerId).build();

        // Should NOT be 401 or 403 (will return 404 since booking does not exist in DB, proving auth passed)
        mockMvc.perform(post("/internal/v1/bookings/" + bookingId + "/apply-payment-pending")
                .header("Authorization", "Bearer " + serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, s, "Must not be 401");
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, s, "Must not be 403");
                });

        PaymentOutcomeRequest outcomeReq = PaymentOutcomeRequest.builder()
                .paymentId(UUID.randomUUID())
                .outcome("SUCCESS")
                .build();

        mockMvc.perform(post("/internal/v1/bookings/" + bookingId + "/payment-outcome")
                .header("Authorization", "Bearer " + serviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outcomeReq)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, s, "Must not be 401");
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, s, "Must not be 403");
                });
    }

    @Test
    @DisplayName("Regression: Admin token is authorized on Booking internal endpoints")
    void testAdminTokenInternalEndpointsAllowed() throws Exception {
        UUID bookingId = UUID.randomUUID();
        ApplyPaymentPendingRequest req = ApplyPaymentPendingRequest.builder().userId(customerId).build();

        mockMvc.perform(post("/internal/v1/bookings/" + bookingId + "/apply-payment-pending")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, s, "Must not be 401");
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, s, "Must not be 403");
                });
    }

    @Test
    @DisplayName("Regression: Malformed and expired tokens return 401 Unauthorized")
    void testMalformedAndExpiredTokensReturn401() throws Exception {
        mockMvc.perform(post("/internal/v1/bookings/" + UUID.randomUUID() + "/apply-payment-pending")
                .header("Authorization", "Bearer invalid.malformed.jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        JwtService expiredJwtService = new JwtService("test-secret-key-for-booking-service-at-least-32-chars", -5000);
        String expiredToken = expiredJwtService.generateToken(UUID.randomUUID(), List.of("SERVICE"));

        mockMvc.perform(post("/internal/v1/bookings/" + UUID.randomUUID() + "/apply-payment-pending")
                .header("Authorization", "Bearer " + expiredToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Regression: Missing, blank, or short secret causes initialization failure")
    void testJwtSecretValidation() {
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 3600000));
        assertThrows(IllegalStateException.class, () -> new JwtService("", 3600000));
        assertThrows(IllegalStateException.class, () -> new JwtService("   ", 3600000));
        assertThrows(IllegalStateException.class, () -> new JwtService("short-secret-under-32-bytes", 3600000));
    }

    @Test
    @DisplayName("Regression: Token signed with different key fails validation")
    void testTokenSignedWithDifferentKeyFailsValidation() {
        JwtService otherJwt = new JwtService("AnotherSecretKeyThatIsAtLeast32BytesLongForTestingPurposes!", 3600000);
        String token = otherJwt.generateToken(UUID.randomUUID(), List.of("SERVICE"));
        assertFalse(jwtService.validateToken(token));
    }
}

