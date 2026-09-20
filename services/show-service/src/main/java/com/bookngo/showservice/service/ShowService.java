package com.bookngo.showservice.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowPricingRepository showPricingRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final OperatorAuthorizationService operatorAuthorizationService;
    private final TheatreServiceClient theatreServiceClient;
    private final MovieServiceClient movieServiceClient;

    public ShowService(
            ShowRepository showRepository,
            ShowPricingRepository showPricingRepository,
            CancellationPolicyRepository cancellationPolicyRepository,
            OperatorAuthorizationService operatorAuthorizationService,
            TheatreServiceClient theatreServiceClient,
            MovieServiceClient movieServiceClient) {
        this.showRepository = showRepository;
        this.showPricingRepository = showPricingRepository;
        this.cancellationPolicyRepository = cancellationPolicyRepository;
        this.operatorAuthorizationService = operatorAuthorizationService;
        this.theatreServiceClient = theatreServiceClient;
        this.movieServiceClient = movieServiceClient;
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> searchShows(
            UUID movieId,
            UUID theatreId,
            String city,
            LocalDate date,
            String language,
            String format) {

        List<UUID> cityTheatreIds = null;
        if (city != null && !city.trim().isEmpty()) {
            cityTheatreIds = theatreServiceClient.getTheatreIdsByCity(city.trim());
            if (cityTheatreIds.isEmpty()) {
                return Collections.emptyList();
            }
        }

        List<UUID> languageMovieIds = null;
        if (language != null && !language.trim().isEmpty()) {
            languageMovieIds = movieServiceClient.getMovieIdsByLanguage(language.trim());
            if (languageMovieIds.isEmpty()) {
                return Collections.emptyList();
            }
        }

        OffsetDateTime startFrom = null;
        OffsetDateTime startTo = null;
        if (date != null) {
            startFrom = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            startTo = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        }

        org.springframework.data.jpa.domain.Specification<Show> spec = com.bookngo.showservice.repository.ShowSpecification.filterShows(
                ShowStatus.SCHEDULED,
                movieId,
                theatreId,
                cityTheatreIds,
                languageMovieIds,
                startFrom,
                startTo,
                format != null ? format.trim() : null);

        List<Show> shows = showRepository.findAll(spec);

        return shows.stream().map(this::toShowResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShowResponse getShowById(UUID showId) {
        Show show = findShowOrThrow(showId);
        return toShowResponse(show);
    }

    public ShowResponse createShow(ShowCreateRequest request, UUID userId, boolean isAdmin) {
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new BadRequestException("endsAt must be after startsAt");
        }

        operatorAuthorizationService.verifyOperatorAssignedToTheatre(request.getTheatreId(), userId, isAdmin);

        boolean overlapping = showRepository.existsOverlappingShow(
                request.getScreenId(),
                ShowStatus.SCHEDULED,
                request.getStartsAt(),
                request.getEndsAt());

        if (overlapping) {
            throw new BadRequestException("Screen " + request.getScreenId() + " already has a show scheduled in the requested time interval");
        }

        Show show = Show.builder()
                .movieId(request.getMovieId())
                .theatreId(request.getTheatreId())
                .screenId(request.getScreenId())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .showFormat(request.getShowFormat() != null ? request.getShowFormat().trim() : null)
                .status(ShowStatus.SCHEDULED)
                .build();

        Show savedShow = showRepository.save(show);
        return toShowResponse(savedShow);
    }

    @Transactional(readOnly = true)
    public List<ShowPricingResponse> getShowPricing(UUID showId) {
        findShowOrThrow(showId);
        List<ShowPricing> pricingList = showPricingRepository.findByShow_ShowIdAndStatus(showId, PricingStatus.ACTIVE);
        return pricingList.stream().map(this::toShowPricingResponse).toList();
    }

    public List<ShowPricingResponse> configureShowPricing(
            UUID showId,
            ConfigurePricingRequest request,
            UUID userId,
            boolean isAdmin) {

        Show show = findShowOrThrow(showId);
        operatorAuthorizationService.verifyOperatorAssignedToTheatre(show.getTheatreId(), userId, isAdmin);

        List<ShowPricingResponse> responses = new ArrayList<>();

        for (ShowPricingUpdateRequest item : request.getPricing()) {
            String category = item.getSeatCategory().trim().toUpperCase();

            Optional<ShowPricing> existingOpt = showPricingRepository
                    .findByShow_ShowIdAndSeatCategoryAndStatus(showId, category, PricingStatus.ACTIVE);

            ShowPricing pricing;
            if (existingOpt.isPresent()) {
                pricing = existingOpt.get();
                pricing.setAmount(item.getAmount());
                pricing.setCurrency(item.getCurrency().trim().toUpperCase());
            } else {
                pricing = ShowPricing.builder()
                        .show(show)
                        .seatCategory(category)
                        .amount(item.getAmount())
                        .currency(item.getCurrency().trim().toUpperCase())
                        .status(PricingStatus.ACTIVE)
                        .build();
            }

            ShowPricing saved = showPricingRepository.save(pricing);
            responses.add(toShowPricingResponse(saved));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public CancellationPolicyResponse getCancellationPolicy(UUID showId) {
        findShowOrThrow(showId);
        CancellationPolicy policy = cancellationPolicyRepository.findByShow_ShowId(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Cancellation policy not found for show: " + showId));
        return toCancellationPolicyResponse(policy);
    }

    public CancellationPolicyResponse configureCancellationPolicy(
            UUID showId,
            CancellationPolicyUpdateRequest request,
            UUID userId,
            boolean isAdmin) {

        Show show = findShowOrThrow(showId);
        operatorAuthorizationService.verifyOperatorAssignedToTheatre(show.getTheatreId(), userId, isAdmin);

        // Invariant check: CANCELLABLE requires cancellationDeadline; NON_CANCELLABLE must have null deadline
        if (request.getPolicyType() == PolicyType.CANCELLABLE) {
            if (request.getCancellationDeadline() == null) {
                throw new BadRequestException("cancellationDeadline is required when policyType is CANCELLABLE");
            }
            if (request.getCancellationDeadline().isAfter(show.getStartsAt())) {
                throw new BadRequestException("cancellationDeadline must be before show start time");
            }
        } else if (request.getPolicyType() == PolicyType.NON_CANCELLABLE) {
            if (request.getCancellationDeadline() != null) {
                // Nullify or enforce invariant
                request.setCancellationDeadline(null);
            }
        }

        Optional<CancellationPolicy> existingOpt = cancellationPolicyRepository.findByShow_ShowId(showId);

        CancellationPolicy policy;
        if (existingOpt.isPresent()) {
            policy = existingOpt.get();
            policy.setPolicyType(request.getPolicyType());
            policy.setCancellationDeadline(request.getCancellationDeadline());
            policy.setStatus(PolicyStatus.ACTIVE);
        } else {
            policy = CancellationPolicy.builder()
                    .show(show)
                    .policyType(request.getPolicyType())
                    .cancellationDeadline(request.getCancellationDeadline())
                    .status(PolicyStatus.ACTIVE)
                    .build();
        }

        CancellationPolicy saved = cancellationPolicyRepository.save(policy);
        return toCancellationPolicyResponse(saved);
    }

    private Show findShowOrThrow(UUID showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with ID: " + showId));
    }

    public ShowResponse toShowResponse(Show show) {
        return ShowResponse.builder()
                .showId(show.getShowId())
                .movieId(show.getMovieId())
                .theatreId(show.getTheatreId())
                .screenId(show.getScreenId())
                .startsAt(show.getStartsAt())
                .endsAt(show.getEndsAt())
                .showFormat(show.getShowFormat())
                .status(show.getStatus())
                .build();
    }

    public ShowPricingResponse toShowPricingResponse(ShowPricing pricing) {
        return ShowPricingResponse.builder()
                .showPricingId(pricing.getShowPricingId())
                .showId(pricing.getShow().getShowId())
                .seatCategory(pricing.getSeatCategory())
                .amount(pricing.getAmount())
                .currency(pricing.getCurrency())
                .status(pricing.getStatus())
                .build();
    }

    public CancellationPolicyResponse toCancellationPolicyResponse(CancellationPolicy policy) {
        return CancellationPolicyResponse.builder()
                .cancellationPolicyId(policy.getCancellationPolicyId())
                .showId(policy.getShow().getShowId())
                .policyType(policy.getPolicyType())
                .cancellationDeadline(policy.getCancellationDeadline())
                .status(policy.getStatus())
                .build();
    }
}
