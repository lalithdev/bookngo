package com.bookngo.showservice.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.showservice.dto.CancellationPolicyUpdateRequest;
import com.bookngo.showservice.dto.ConfigurePricingRequest;
import com.bookngo.showservice.dto.ShowCreateRequest;
import com.bookngo.showservice.dto.ShowPricingUpdateRequest;
import com.bookngo.showservice.entity.CancellationPolicy;
import com.bookngo.showservice.entity.PolicyStatus;
import com.bookngo.showservice.entity.PolicyType;
import com.bookngo.showservice.entity.PricingStatus;
import com.bookngo.showservice.entity.Show;
import com.bookngo.showservice.entity.ShowPricing;
import com.bookngo.showservice.entity.ShowStatus;
import com.bookngo.showservice.repository.CancellationPolicyRepository;
import com.bookngo.showservice.repository.ShowPricingRepository;
import com.bookngo.showservice.repository.ShowRepository;
import com.bookngo.showservice.security.JwtService;
import com.bookngo.showservice.security.OperatorAuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowPricingRepository showPricingRepository;

    @Autowired
    private CancellationPolicyRepository cancellationPolicyRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OperatorAuthorizationService operatorAuthorizationService;

    private UUID movieId;
    private UUID theatreId1;
    private UUID theatreId2;
    private UUID screenId;
    private Show savedShow;

    private UUID operator1Id;
    private UUID operator2Id;
    private UUID adminId;
    private UUID customerId;

    private String operator1Token;
    private String operator2Token;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        cancellationPolicyRepository.deleteAll();
        showPricingRepository.deleteAll();
        showRepository.deleteAll();
        operatorAuthorizationService.clearAssignments();

        movieId = UUID.randomUUID();
        theatreId1 = UUID.randomUUID();
        theatreId2 = UUID.randomUUID();
        screenId = UUID.randomUUID();

        operator1Id = UUID.randomUUID();
        operator2Id = UUID.randomUUID();
        adminId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        operator1Token = jwtService.generateToken(operator1Id, List.of("THEATRE_OPERATOR"));
        operator2Token = jwtService.generateToken(operator2Id, List.of("THEATRE_OPERATOR"));
        adminToken = jwtService.generateToken(adminId, List.of("ADMIN"));
        customerToken = jwtService.generateToken(customerId, List.of("CUSTOMER"));

        // Assign operator1 to theatreId1 (operator2 is unassigned)
        operatorAuthorizationService.registerAssignment(operator1Id, theatreId1);

        savedShow = showRepository.save(Show.builder()
                .movieId(movieId)
                .theatreId(theatreId1)
                .screenId(screenId)
                .startsAt(OffsetDateTime.now().plusDays(2))
                .endsAt(OffsetDateTime.now().plusDays(2).plusHours(2).plusMinutes(30))
                .showFormat("IMAX")
                .status(ShowStatus.SCHEDULED)
                .build());
    }

    @Test
    void searchShows_public_returnsMatchingShows() throws Exception {
        mockMvc.perform(get("/api/v1/shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].showId", is(savedShow.getShowId().toString())))
                .andExpect(jsonPath("$[0].showFormat", is("IMAX")));
    }

    @Test
    void searchShows_filterByMovieId_returnsCorrectShow() throws Exception {
        mockMvc.perform(get("/api/v1/shows").param("movieId", movieId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/shows").param("movieId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createShow_anonymous_returnsUnauthorized() throws Exception {
        ShowCreateRequest req = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId1)
                .screenId(screenId)
                .startsAt(OffsetDateTime.now().plusDays(5))
                .endsAt(OffsetDateTime.now().plusDays(5).plusHours(2))
                .build();

        mockMvc.perform(post("/api/v1/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void createShow_customer_returnsForbidden() throws Exception {
        ShowCreateRequest req = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId1)
                .screenId(screenId)
                .startsAt(OffsetDateTime.now().plusDays(5))
                .endsAt(OffsetDateTime.now().plusDays(5).plusHours(2))
                .build();

        mockMvc.perform(post("/api/v1/shows")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void createShow_unassignedOperator_returnsForbidden() throws Exception {
        ShowCreateRequest req = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId1)
                .screenId(screenId)
                .startsAt(OffsetDateTime.now().plusDays(5))
                .endsAt(OffsetDateTime.now().plusDays(5).plusHours(2))
                .build();

        mockMvc.perform(post("/api/v1/shows")
                        .header("Authorization", "Bearer " + operator2Token) // operator2 is unassigned
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void createShow_assignedOperator_createsShow() throws Exception {
        ShowCreateRequest req = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId1)
                .screenId(screenId)
                .startsAt(OffsetDateTime.now().plusDays(5))
                .endsAt(OffsetDateTime.now().plusDays(5).plusHours(2))
                .showFormat("2D")
                .build();

        mockMvc.perform(post("/api/v1/shows")
                        .header("Authorization", "Bearer " + operator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.theatreId", is(theatreId1.toString())))
                .andExpect(jsonPath("$.showFormat", is("2D")))
                .andExpect(jsonPath("$.status", is("SCHEDULED")));
    }

    @Test
    void createShow_admin_createsShow() throws Exception {
        ShowCreateRequest req = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId2) // theatre2 where no operator is assigned
                .screenId(UUID.randomUUID())
                .startsAt(OffsetDateTime.now().plusDays(6))
                .endsAt(OffsetDateTime.now().plusDays(6).plusHours(2))
                .showFormat("3D")
                .build();

        mockMvc.perform(post("/api/v1/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.theatreId", is(theatreId2.toString())))
                .andExpect(jsonPath("$.showFormat", is("3D")));
    }

    @Test
    void createShow_invalidTimeRange_returnsBadRequest() throws Exception {
        ShowCreateRequest req = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId1)
                .screenId(screenId)
                .startsAt(OffsetDateTime.now().plusDays(5).plusHours(2))
                .endsAt(OffsetDateTime.now().plusDays(5)) // ends before starts
                .build();

        mockMvc.perform(post("/api/v1/shows")
                        .header("Authorization", "Bearer " + operator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    void getShowById_existingShow_returnsShow() throws Exception {
        mockMvc.perform(get("/api/v1/shows/" + savedShow.getShowId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showId", is(savedShow.getShowId().toString())))
                .andExpect(jsonPath("$.showFormat", is("IMAX")));
    }

    @Test
    void getShowById_nonexistentShow_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/shows/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    void configureAndGetShowPricing_success() throws Exception {
        ConfigurePricingRequest pricingReq = ConfigurePricingRequest.builder()
                .pricing(List.of(
                        new ShowPricingUpdateRequest("REGULAR", new BigDecimal("150.00"), "INR"),
                        new ShowPricingUpdateRequest("PREMIUM", new BigDecimal("250.00"), "INR"),
                        new ShowPricingUpdateRequest("VIP", new BigDecimal("400.00"), "INR")
                ))
                .build();

        // 1. Configure pricing as assigned operator
        mockMvc.perform(put("/api/v1/shows/" + savedShow.getShowId() + "/pricing")
                        .header("Authorization", "Bearer " + operator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pricingReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // 2. Fetch pricing publicly
        mockMvc.perform(get("/api/v1/shows/" + savedShow.getShowId() + "/pricing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void configureShowPricing_unauthorized_returnsForbidden() throws Exception {
        ConfigurePricingRequest pricingReq = ConfigurePricingRequest.builder()
                .pricing(List.of(new ShowPricingUpdateRequest("REGULAR", new BigDecimal("150.00"), "INR")))
                .build();

        mockMvc.perform(put("/api/v1/shows/" + savedShow.getShowId() + "/pricing")
                        .header("Authorization", "Bearer " + operator2Token) // unassigned
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pricingReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void configureAndGetCancellationPolicy_success() throws Exception {
        OffsetDateTime deadline = savedShow.getStartsAt().minusHours(4);
        CancellationPolicyUpdateRequest policyReq = CancellationPolicyUpdateRequest.builder()
                .policyType(PolicyType.CANCELLABLE)
                .cancellationDeadline(deadline)
                .build();

        // 1. Configure as assigned operator
        mockMvc.perform(put("/api/v1/shows/" + savedShow.getShowId() + "/cancellation-policy")
                        .header("Authorization", "Bearer " + operator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyType", is("CANCELLABLE")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // 2. Public get
        mockMvc.perform(get("/api/v1/shows/" + savedShow.getShowId() + "/cancellation-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyType", is("CANCELLABLE")));

        // 3. Internal get
        mockMvc.perform(get("/internal/v1/shows/" + savedShow.getShowId() + "/cancellation-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyType", is("CANCELLABLE")));
    }

    @Test
    void configureCancellationPolicy_cancellableWithoutDeadline_returnsBadRequest() throws Exception {
        CancellationPolicyUpdateRequest policyReq = CancellationPolicyUpdateRequest.builder()
                .policyType(PolicyType.CANCELLABLE)
                .cancellationDeadline(null)
                .build();

        mockMvc.perform(put("/api/v1/shows/" + savedShow.getShowId() + "/cancellation-policy")
                        .header("Authorization", "Bearer " + operator1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }
}
