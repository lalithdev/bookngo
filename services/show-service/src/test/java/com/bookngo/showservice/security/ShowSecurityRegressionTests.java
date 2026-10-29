package com.bookngo.showservice.security;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.showservice.dto.ShowCreateRequest;
import com.bookngo.showservice.entity.CancellationPolicy;
import com.bookngo.showservice.entity.PolicyStatus;
import com.bookngo.showservice.entity.PolicyType;
import com.bookngo.showservice.entity.Show;
import com.bookngo.showservice.entity.ShowStatus;
import com.bookngo.showservice.repository.CancellationPolicyRepository;
import com.bookngo.showservice.repository.ShowPricingRepository;
import com.bookngo.showservice.repository.ShowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShowSecurityRegressionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowPricingRepository showPricingRepository;

    @Autowired
    private CancellationPolicyRepository policyRepository;

    @Autowired
    private OperatorAuthorizationService operatorAuthService;

    private UUID showId;
    private UUID theatreId;
    private UUID operatorId;
    private String customerToken;
    private String serviceToken;
    private String adminToken;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
        showPricingRepository.deleteAll();
        showRepository.deleteAll();
        operatorAuthService.clearAssignments();

        theatreId = UUID.randomUUID();
        operatorId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        customerToken = jwtService.generateToken(customerId, List.of("CUSTOMER"));
        serviceToken = jwtService.generateToken(UUID.fromString("00000000-0000-0000-0000-000000000005"), List.of("SERVICE"));
        adminToken = jwtService.generateToken(UUID.randomUUID(), List.of("ADMIN"));
        operatorToken = jwtService.generateToken(operatorId, List.of("THEATRE_OPERATOR"));

        Show show = Show.builder()
                .theatreId(theatreId)
                .screenId(UUID.randomUUID())
                .movieId(UUID.randomUUID())
                .startsAt(OffsetDateTime.now().plusDays(1))
                .endsAt(OffsetDateTime.now().plusDays(1).plusHours(2))
                .showFormat("2D")
                .status(ShowStatus.SCHEDULED)
                .build();
        show = showRepository.save(show);
        showId = show.getShowId();

        CancellationPolicy policy = CancellationPolicy.builder()
                .show(show)
                .policyType(PolicyType.CANCELLABLE)
                .cancellationDeadline(OffsetDateTime.now().plusHours(12))
                .status(PolicyStatus.ACTIVE)
                .build();
        policyRepository.save(policy);
    }

    @Test
    @DisplayName("Regression: Unauthenticated request to internal show endpoint must return 401 Unauthorized")
    void testUnauthenticatedInternalEndpointFailsWith401() throws Exception {
        mockMvc.perform(get("/internal/v1/shows/" + showId + "/cancellation-policy"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Regression: Customer token requesting internal show endpoint must return 403 Forbidden")
    void testCustomerTokenInternalEndpointFailsWith403() throws Exception {
        mockMvc.perform(get("/internal/v1/shows/" + showId + "/cancellation-policy")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Regression: Service token requesting internal show endpoint succeeds with 200 OK")
    void testServiceTokenInternalEndpointSucceedsWith200() throws Exception {
        mockMvc.perform(get("/internal/v1/shows/" + showId + "/cancellation-policy")
                .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showId").value(showId.toString()));
    }

    @Test
    @DisplayName("Regression: Admin token requesting internal show endpoint succeeds with 200 OK")
    void testAdminTokenInternalEndpointSucceedsWith200() throws Exception {
        mockMvc.perform(get("/internal/v1/shows/" + showId + "/cancellation-policy")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showId").value(showId.toString()));
    }

    @Test
    @DisplayName("Regression: Forged X-Operator-Assigned-Theatres header cannot bypass theatre operator authorization")
    void testForgedOperatorHeaderCannotBypassAuthorization() throws Exception {
        // Operator is NOT assigned to theatreId
        ShowCreateRequest request = ShowCreateRequest.builder()
                .theatreId(theatreId)
                .screenId(UUID.randomUUID())
                .movieId(UUID.randomUUID())
                .startsAt(OffsetDateTime.now().plusDays(2))
                .endsAt(OffsetDateTime.now().plusDays(2).plusHours(2))
                .showFormat("2D")
                .build();

        // Attempt creation with forged header claiming assignment to theatreId
        mockMvc.perform(post("/api/v1/shows")
                .header("Authorization", "Bearer " + operatorToken)
                .header("X-Operator-Assigned-Theatres", theatreId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Regression: Malformed and expired tokens return 401 Unauthorized")
    void testMalformedAndExpiredTokensReturn401() throws Exception {
        mockMvc.perform(get("/internal/v1/shows/" + showId + "/cancellation-policy")
                .header("Authorization", "Bearer malformed.token.value"))
                .andExpect(status().isUnauthorized());

        JwtService shortLivedJwt = new JwtService("test-secret-key-for-show-service-tests-at-least-32-chars", -5000);
        String expiredToken = shortLivedJwt.generateToken(UUID.randomUUID(), List.of("SERVICE"));

        mockMvc.perform(get("/internal/v1/shows/" + showId + "/cancellation-policy")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Regression: Missing, blank or short JWT secret causes initialization failure")
    void testJwtSecretValidation() {
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 3600000));
        assertThrows(IllegalStateException.class, () -> new JwtService("", 3600000));
        assertThrows(IllegalStateException.class, () -> new JwtService("   ", 3600000));
        assertThrows(IllegalStateException.class, () -> new JwtService("short-key", 3600000));
    }
}
