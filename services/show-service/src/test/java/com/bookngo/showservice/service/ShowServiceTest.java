package com.bookngo.showservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.bookngo.showservice.dto.CancellationPolicyResponse;
import com.bookngo.showservice.dto.CancellationPolicyUpdateRequest;
import com.bookngo.showservice.dto.ConfigurePricingRequest;
import com.bookngo.showservice.dto.ShowCreateRequest;
import com.bookngo.showservice.dto.ShowPricingResponse;
import com.bookngo.showservice.dto.ShowPricingUpdateRequest;
import com.bookngo.showservice.dto.ShowResponse;
import com.bookngo.showservice.entity.CancellationPolicy;
import com.bookngo.showservice.entity.PolicyStatus;
import com.bookngo.showservice.entity.PolicyType;
import com.bookngo.showservice.entity.PricingStatus;
import com.bookngo.showservice.entity.Show;
import com.bookngo.showservice.entity.ShowPricing;
import com.bookngo.showservice.entity.ShowStatus;
import com.bookngo.showservice.exception.BadRequestException;
import com.bookngo.showservice.exception.ResourceNotFoundException;
import com.bookngo.showservice.repository.CancellationPolicyRepository;
import com.bookngo.showservice.repository.ShowPricingRepository;
import com.bookngo.showservice.repository.ShowRepository;
import com.bookngo.showservice.security.OperatorAuthorizationService;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private ShowPricingRepository showPricingRepository;

    @Mock
    private CancellationPolicyRepository cancellationPolicyRepository;

    @Mock
    private OperatorAuthorizationService operatorAuthorizationService;

    @Mock
    private TheatreServiceClient theatreServiceClient;

    @Mock
    private MovieServiceClient movieServiceClient;

    @InjectMocks
    private ShowService showService;

    private UUID showId;
    private UUID movieId;
    private UUID theatreId;
    private UUID screenId;
    private UUID operatorId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private Show testShow;

    @BeforeEach
    void setUp() {
        showId = UUID.randomUUID();
        movieId = UUID.randomUUID();
        theatreId = UUID.randomUUID();
        screenId = UUID.randomUUID();
        operatorId = UUID.randomUUID();
        startsAt = OffsetDateTime.now().plusDays(1);
        endsAt = startsAt.plusHours(2).plusMinutes(30);

        testShow = Show.builder()
                .showId(showId)
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .showFormat("IMAX")
                .status(ShowStatus.SCHEDULED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void searchShows_withoutFilters_returnsScheduledShows() {
        when(showRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(testShow));

        List<ShowResponse> results = showService.searchShows(null, null, null, null, null, null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(showId, results.get(0).getShowId());
    }

    @Test
    void searchShows_withCityFilter_resolvesTheatreIds() {
        when(theatreServiceClient.getTheatreIdsByCity("Bengaluru")).thenReturn(List.of(theatreId));
        when(showRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(testShow));

        List<ShowResponse> results = showService.searchShows(null, null, "Bengaluru", null, null, null);

        assertEquals(1, results.size());
        verify(theatreServiceClient).getTheatreIdsByCity("Bengaluru");
    }

    @Test
    void searchShows_withCityFilterNoTheatres_returnsEmpty() {
        when(theatreServiceClient.getTheatreIdsByCity("UnknownCity")).thenReturn(List.of());

        List<ShowResponse> results = showService.searchShows(null, null, "UnknownCity", null, null, null);

        assertEquals(0, results.size());
    }

    @Test
    void searchShows_withLanguageFilter_resolvesMovieIds() {
        when(movieServiceClient.getMovieIdsByLanguage("English")).thenReturn(List.of(movieId));
        when(showRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(testShow));

        List<ShowResponse> results = showService.searchShows(null, null, null, null, "English", null);

        assertEquals(1, results.size());
        verify(movieServiceClient).getMovieIdsByLanguage("English");
    }

    @Test
    void searchShows_withDateFilter_convertsToOffsetRange() {
        LocalDate date = LocalDate.of(2026, 9, 21);
        when(showRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(testShow));

        List<ShowResponse> results = showService.searchShows(null, null, null, date, null, null);

        assertNotNull(results);
    }

    @Test
    void getShowById_existingShow_returnsShow() {
        when(showRepository.findById(showId)).thenReturn(Optional.of(testShow));

        ShowResponse response = showService.getShowById(showId);

        assertNotNull(response);
        assertEquals(showId, response.getShowId());
        assertEquals("IMAX", response.getShowFormat());
    }

    @Test
    void getShowById_nonexistentShow_throwsNotFound() {
        when(showRepository.findById(showId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> showService.getShowById(showId));
    }

    @Test
    void createShow_validRequest_createsSuccessfully() {
        ShowCreateRequest request = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .showFormat("IMAX")
                .build();

        when(showRepository.existsOverlappingShow(eq(screenId), eq(ShowStatus.SCHEDULED), eq(startsAt), eq(endsAt)))
                .thenReturn(false);
        when(showRepository.save(any(Show.class))).thenAnswer(inv -> {
            Show s = inv.getArgument(0);
            s.setShowId(showId);
            return s;
        });

        ShowResponse response = showService.createShow(request, operatorId, false);

        assertNotNull(response);
        assertEquals(showId, response.getShowId());
        verify(operatorAuthorizationService).verifyOperatorAssignedToTheatre(theatreId, operatorId, false);
    }

    @Test
    void createShow_invalidTimeInterval_throwsBadRequest() {
        ShowCreateRequest request = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startsAt(endsAt)
                .endsAt(startsAt) // ends before starts
                .build();

        assertThrows(BadRequestException.class, () -> showService.createShow(request, operatorId, false));
    }

    @Test
    void createShow_unassignedOperator_throwsAccessDenied() {
        ShowCreateRequest request = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        doThrow(new AccessDeniedException("Theatre operator is not assigned to this theatre"))
                .when(operatorAuthorizationService).verifyOperatorAssignedToTheatre(theatreId, operatorId, false);

        assertThrows(AccessDeniedException.class, () -> showService.createShow(request, operatorId, false));
    }

    @Test
    void createShow_screenOverlap_throwsBadRequest() {
        ShowCreateRequest request = ShowCreateRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        when(showRepository.existsOverlappingShow(eq(screenId), eq(ShowStatus.SCHEDULED), eq(startsAt), eq(endsAt)))
                .thenReturn(true);

        assertThrows(BadRequestException.class, () -> showService.createShow(request, operatorId, false));
    }

    @Test
    void getShowPricing_existingShow_returnsPricing() {
        when(showRepository.findById(showId)).thenReturn(Optional.of(testShow));
        ShowPricing pricing = ShowPricing.builder()
                .showPricingId(UUID.randomUUID())
                .show(testShow)
                .seatCategory("REGULAR")
                .amount(new BigDecimal("250.00"))
                .currency("INR")
                .status(PricingStatus.ACTIVE)
                .build();
        when(showPricingRepository.findByShow_ShowIdAndStatus(showId, PricingStatus.ACTIVE))
                .thenReturn(List.of(pricing));

        List<ShowPricingResponse> list = showService.getShowPricing(showId);

        assertEquals(1, list.size());
        assertEquals("REGULAR", list.get(0).getSeatCategory());
        assertEquals(new BigDecimal("250.00"), list.get(0).getAmount());
    }

    @Test
    void configureShowPricing_authorizedOperator_configuresSuccessfully() {
        when(showRepository.findById(showId)).thenReturn(Optional.of(testShow));
        ConfigurePricingRequest request = ConfigurePricingRequest.builder()
                .pricing(List.of(
                        new ShowPricingUpdateRequest("REGULAR", new BigDecimal("200.00"), "INR"),
                        new ShowPricingUpdateRequest("PREMIUM", new BigDecimal("350.00"), "INR")
                ))
                .build();

        when(showPricingRepository.findByShow_ShowIdAndSeatCategoryAndStatus(eq(showId), any(), eq(PricingStatus.ACTIVE)))
                .thenReturn(Optional.empty());
        when(showPricingRepository.save(any(ShowPricing.class))).thenAnswer(inv -> {
            ShowPricing p = inv.getArgument(0);
            p.setShowPricingId(UUID.randomUUID());
            return p;
        });

        List<ShowPricingResponse> responses = showService.configureShowPricing(showId, request, operatorId, false);

        assertEquals(2, responses.size());
        verify(operatorAuthorizationService).verifyOperatorAssignedToTheatre(theatreId, operatorId, false);
    }

    @Test
    void getCancellationPolicy_existingPolicy_returnsPolicy() {
        when(showRepository.findById(showId)).thenReturn(Optional.of(testShow));
        CancellationPolicy policy = CancellationPolicy.builder()
                .cancellationPolicyId(UUID.randomUUID())
                .show(testShow)
                .policyType(PolicyType.CANCELLABLE)
                .cancellationDeadline(startsAt.minusHours(2))
                .status(PolicyStatus.ACTIVE)
                .build();
        when(cancellationPolicyRepository.findByShow_ShowId(showId)).thenReturn(Optional.of(policy));

        CancellationPolicyResponse response = showService.getCancellationPolicy(showId);

        assertNotNull(response);
        assertEquals(PolicyType.CANCELLABLE, response.getPolicyType());
    }

    @Test
    void configureCancellationPolicy_cancellableWithoutDeadline_throwsBadRequest() {
        when(showRepository.findById(showId)).thenReturn(Optional.of(testShow));
        CancellationPolicyUpdateRequest req = new CancellationPolicyUpdateRequest(PolicyType.CANCELLABLE, null);

        assertThrows(BadRequestException.class,
                () -> showService.configureCancellationPolicy(showId, req, operatorId, false));
    }

    @Test
    void configureCancellationPolicy_deadlineAfterStart_throwsBadRequest() {
        when(showRepository.findById(showId)).thenReturn(Optional.of(testShow));
        CancellationPolicyUpdateRequest req = new CancellationPolicyUpdateRequest(
                PolicyType.CANCELLABLE, startsAt.plusHours(1));

        assertThrows(BadRequestException.class,
                () -> showService.configureCancellationPolicy(showId, req, operatorId, false));
    }

    @Test
    void configureCancellationPolicy_validPolicy_configuresSuccessfully() {
        when(showRepository.findById(showId)).thenReturn(Optional.of(testShow));
        CancellationPolicyUpdateRequest req = new CancellationPolicyUpdateRequest(
                PolicyType.CANCELLABLE, startsAt.minusHours(4));

        when(cancellationPolicyRepository.findByShow_ShowId(showId)).thenReturn(Optional.empty());
        when(cancellationPolicyRepository.save(any(CancellationPolicy.class))).thenAnswer(inv -> {
            CancellationPolicy cp = inv.getArgument(0);
            cp.setCancellationPolicyId(UUID.randomUUID());
            return cp;
        });

        CancellationPolicyResponse response = showService.configureCancellationPolicy(showId, req, operatorId, false);

        assertNotNull(response);
        assertEquals(PolicyType.CANCELLABLE, response.getPolicyType());
        verify(operatorAuthorizationService).verifyOperatorAssignedToTheatre(theatreId, operatorId, false);
    }
}
