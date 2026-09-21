package com.bookngo.bookingservice.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.bookngo.bookingservice.client.PaymentServiceClient;
import com.bookngo.bookingservice.client.ShowServiceClient;
import com.bookngo.bookingservice.client.TheatreServiceClient;
import com.bookngo.bookingservice.dto.ApplyPaymentPendingInternalResponse;
import com.bookngo.bookingservice.dto.ApplyPaymentPendingRequest;
import com.bookngo.bookingservice.dto.BookingResponse;
import com.bookngo.bookingservice.dto.BookingSeatResponse;
import com.bookngo.bookingservice.dto.CancellationPolicyDto;
import com.bookngo.bookingservice.dto.PaymentOutcomeInternalResponse;
import com.bookngo.bookingservice.dto.PaymentOutcomeRequest;
import com.bookngo.bookingservice.dto.PhysicalSeatDto;
import com.bookngo.bookingservice.dto.SeatHoldDto;
import com.bookngo.bookingservice.dto.SeatHoldRequest;
import com.bookngo.bookingservice.dto.SeatHoldResponse;
import com.bookngo.bookingservice.dto.SeatInventoryItem;
import com.bookngo.bookingservice.dto.ShowDto;
import com.bookngo.bookingservice.dto.ShowPricingDto;
import com.bookngo.bookingservice.dto.TicketResponse;
import com.bookngo.bookingservice.entity.Booking;
import com.bookngo.bookingservice.entity.BookingIdempotencyRecord;
import com.bookngo.bookingservice.entity.BookingSeat;
import com.bookngo.bookingservice.entity.BookingStatus;
import com.bookngo.bookingservice.entity.HoldStatus;
import com.bookngo.bookingservice.entity.IdempotencyStatus;
import com.bookngo.bookingservice.entity.InventoryStatus;
import com.bookngo.bookingservice.entity.SeatHold;
import com.bookngo.bookingservice.entity.ShowSeatInventory;
import com.bookngo.bookingservice.entity.Ticket;
import com.bookngo.bookingservice.entity.TicketStatus;
import com.bookngo.bookingservice.exception.BadRequestException;
import com.bookngo.bookingservice.exception.ConflictException;
import com.bookngo.bookingservice.exception.ForbiddenException;
import com.bookngo.bookingservice.exception.ResourceNotFoundException;
import com.bookngo.bookingservice.exception.UnprocessableEntityException;
import com.bookngo.bookingservice.repository.BookingIdempotencyRecordRepository;
import com.bookngo.bookingservice.repository.BookingRepository;
import com.bookngo.bookingservice.repository.BookingSeatRepository;
import com.bookngo.bookingservice.repository.SeatHoldRepository;
import com.bookngo.bookingservice.repository.ShowSeatInventoryRepository;
import com.bookngo.bookingservice.repository.TicketRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatInventoryRepository showSeatInventoryRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final TicketRepository ticketRepository;
    private final BookingIdempotencyRecordRepository idempotencyRecordRepository;
    private final TheatreServiceClient theatreServiceClient;
    private final ShowServiceClient showServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final ObjectMapper objectMapper;
    private final javax.sql.DataSource dataSource;
    private final boolean isPostgres;

    public BookingService(
            BookingRepository bookingRepository,
            SeatHoldRepository seatHoldRepository,
            ShowSeatInventoryRepository showSeatInventoryRepository,
            BookingSeatRepository bookingSeatRepository,
            TicketRepository ticketRepository,
            BookingIdempotencyRecordRepository idempotencyRecordRepository,
            TheatreServiceClient theatreServiceClient,
            ShowServiceClient showServiceClient,
            PaymentServiceClient paymentServiceClient,
            ObjectMapper objectMapper,
            javax.sql.DataSource dataSource) {
        this.bookingRepository = bookingRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.showSeatInventoryRepository = showSeatInventoryRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.ticketRepository = ticketRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.theatreServiceClient = theatreServiceClient;
        this.showServiceClient = showServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;

        boolean isPg = true;
        try (java.sql.Connection conn = dataSource.getConnection()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            isPg = dbProduct != null && dbProduct.toLowerCase().contains("postgres");
        } catch (Exception e) {
            log.warn("Could not determine database product name, defaulting to PostgreSQL", e);
        }
        this.isPostgres = isPg;
    }

    /**
     * Initializes show seat inventory if not already populated.
     * Uses PostgreSQL conflict-safe insertion (ON CONFLICT DO NOTHING) so multiple instances are safe.
     */
    @Transactional
    public void ensureInventoryInitialized(UUID showId) {
        long count = showSeatInventoryRepository.countByShowId(showId);
        if (count > 0) {
            return;
        }

        ShowDto show = showServiceClient.getShow(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        List<PhysicalSeatDto> physicalSeats = theatreServiceClient.getPhysicalSeatsByScreen(show.getScreenId());
        if (physicalSeats.isEmpty()) {
            log.warn("No physical seats found from Theatre Service for screen {}", show.getScreenId());
            return;
        }

        if (this.isPostgres) {
            for (PhysicalSeatDto seat : physicalSeats) {
                showSeatInventoryRepository.insertInventoryIfAbsent(
                        UUID.randomUUID(), showId, seat.getPhysicalSeatId());
            }
        } else {
            // Test / In-memory fallback (H2 compatibility)
            List<ShowSeatInventory> existing = showSeatInventoryRepository.findByShowId(showId);
            Set<UUID> existingSeatIds = existing.stream().map(ShowSeatInventory::getPhysicalSeatId).collect(Collectors.toSet());
            List<ShowSeatInventory> toInsert = new ArrayList<>();
            for (PhysicalSeatDto seat : physicalSeats) {
                if (!existingSeatIds.contains(seat.getPhysicalSeatId())) {
                    toInsert.add(ShowSeatInventory.builder()
                            .showId(showId)
                            .physicalSeatId(seat.getPhysicalSeatId())
                            .status(InventoryStatus.AVAILABLE)
                            .build());
                }
            }
            if (!toInsert.isEmpty()) {
                showSeatInventoryRepository.saveAllAndFlush(toInsert);
            }
        }
    }

    /**
     * 1. GET /api/v1/shows/{showId}/seat-inventory
     * Returns real-time show-specific seat inventory with lazy-expiration awareness.
     */
    @Transactional
    public List<SeatInventoryItem> getShowSeatInventory(UUID showId) {
        ensureInventoryInitialized(showId);

        List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByShowId(showId);
        OffsetDateTime now = OffsetDateTime.now();

        return inventory.stream().map(row -> {
            String effectiveStatus = row.getStatus().name();
            OffsetDateTime holdExpiry = row.getHoldExpiresAt();

            // Lazy expiry representation: an expired HELD seat is available
            if (row.getStatus() == InventoryStatus.HELD && holdExpiry != null && holdExpiry.isBefore(now)) {
                effectiveStatus = InventoryStatus.AVAILABLE.name();
                holdExpiry = null;
            }

            return SeatInventoryItem.builder()
                    .showSeatInventoryId(row.getShowSeatInventoryId())
                    .physicalSeatId(row.getPhysicalSeatId())
                    .status(effectiveStatus)
                    .holdExpiresAt(holdExpiry)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 2. POST /api/v1/shows/{showId}/holds
     * Concurrency-safe hold creation.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SeatHoldResponse createSeatHold(UUID showId, UUID userId, String idempotencyKey, SeatHoldRequest request) {
        if (request == null || request.getPhysicalSeatIds() == null || request.getPhysicalSeatIds().isEmpty()) {
            throw new BadRequestException("physicalSeatIds must not be empty");
        }
        if (request.getPhysicalSeatIds().size() > 6) {
            throw new BadRequestException("Cannot hold more than 6 seats in a single booking");
        }

        // Deduplicate request seat IDs and ensure valid count
        Set<UUID> uniqueSeatIds = new HashSet<>(request.getPhysicalSeatIds());
        if (uniqueSeatIds.size() != request.getPhysicalSeatIds().size()) {
            throw new BadRequestException("Duplicate physical seat IDs provided in request");
        }

        // Canonical deterministic sorting (UUID natural order)
        List<UUID> sortedSeatIds = new ArrayList<>(uniqueSeatIds);
        sortedSeatIds.sort(UUID::compareTo);

        // 1. Idempotency Check
        String requestHash = computeSha256(showId + ":" + sortedSeatIds.toString());
        Optional<BookingIdempotencyRecord> existingRecordOpt =
                idempotencyRecordRepository.findByUserIdAndOperationTypeAndIdempotencyKey(
                        userId, "CREATE_HOLD", idempotencyKey);

        if (existingRecordOpt.isPresent()) {
            BookingIdempotencyRecord existingRecord = existingRecordOpt.get();
            if (!existingRecord.getRequestHash().equals(requestHash)) {
                throw new ConflictException("IDEMPOTENCY_MISMATCH",
                        "Idempotency key reused with a different request payload");
            }
            if (existingRecord.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new ConflictException("OPERATION_IN_PROGRESS",
                        "Operation currently in progress for this idempotency key");
            }
            if (existingRecord.getStatus() == IdempotencyStatus.COMPLETED && existingRecord.getBookingId() != null) {
                return replaySeatHoldResponse(existingRecord.getBookingId());
            }
        }

        // Ensure inventory exists
        ensureInventoryInitialized(showId);

        // 2. Concurrency Control: Acquire PostgreSQL row-level locks in deterministic ascending order
        List<ShowSeatInventory> lockedRows =
                showSeatInventoryRepository.findByShowIdAndPhysicalSeatIdInOrderAscForUpdate(showId, sortedSeatIds);

        if (lockedRows.size() != sortedSeatIds.size()) {
            throw new BadRequestException("One or more requested seats do not exist for show: " + showId);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 3. Validate that every requested seat is either AVAILABLE or previous hold has expired
        for (ShowSeatInventory row : lockedRows) {
            boolean isAvailable = (row.getStatus() == InventoryStatus.AVAILABLE);
            boolean isExpiredHold = (row.getStatus() == InventoryStatus.HELD
                    && row.getHoldExpiresAt() != null
                    && row.getHoldExpiresAt().isBefore(now));

            if (!isAvailable && !isExpiredHold) {
                throw new ConflictException("SEAT_UNAVAILABLE",
                        "One or more requested seats are already held or booked: " + row.getPhysicalSeatId());
            }
        }

        // 4. Fetch Pricing and Seat Details
        ShowDto show = showServiceClient.getShow(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        List<ShowPricingDto> pricingList = showServiceClient.getShowPricing(showId);
        Map<String, BigDecimal> categoryPricing = pricingList.stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toMap(ShowPricingDto::getSeatCategory, ShowPricingDto::getAmount, (a, b) -> a));

        List<PhysicalSeatDto> physicalSeats = theatreServiceClient.getPhysicalSeatsByScreen(show.getScreenId());
        Map<UUID, PhysicalSeatDto> physicalSeatMap = physicalSeats.stream()
                .collect(Collectors.toMap(PhysicalSeatDto::getPhysicalSeatId, s -> s));

        String currency = pricingList.isEmpty() ? "INR" : pricingList.get(0).getCurrency();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 5. Create Booking in INITIATED status
        Booking booking = Booking.builder()
                .userId(userId)
                .showId(showId)
                .totalAmount(BigDecimal.ZERO) // temporary, updated below
                .currency(currency)
                .status(BookingStatus.INITIATED)
                .build();
        booking = bookingRepository.save(booking);

        // 6. Create SeatHold with ACTIVE status and 5-minute expiry
        OffsetDateTime normalExpiresAt = now.plusMinutes(5);
        SeatHold hold = SeatHold.builder()
                .bookingId(booking.getBookingId())
                .userId(userId)
                .showId(showId)
                .status(HoldStatus.ACTIVE)
                .normalExpiresAt(normalExpiresAt)
                .build();
        hold = seatHoldRepository.save(hold);

        // 7. Create BookingSeats and update inventory
        for (ShowSeatInventory row : lockedRows) {
            PhysicalSeatDto seatDto = physicalSeatMap.get(row.getPhysicalSeatId());
            String category = seatDto != null ? seatDto.getSeatCategory() : "REGULAR";
            String label = seatDto != null ? (seatDto.getRowLabel() + seatDto.getSeatNumber()) : "SEAT";
            BigDecimal unitPrice = categoryPricing.getOrDefault(category, BigDecimal.valueOf(150.00));
            totalAmount = totalAmount.add(unitPrice);

            BookingSeat bookingSeat = BookingSeat.builder()
                    .bookingId(booking.getBookingId())
                    .showSeatInventoryId(row.getShowSeatInventoryId())
                    .physicalSeatId(row.getPhysicalSeatId())
                    .seatLabelSnapshot(label)
                    .seatCategorySnapshot(category)
                    .unitPriceSnapshot(unitPrice)
                    .build();
            bookingSeatRepository.save(bookingSeat);

            // Mark inventory as HELD
            row.setStatus(InventoryStatus.HELD);
            row.setActiveHoldId(hold.getHoldId());
            row.setCurrentBookingId(null);
            row.setHoldExpiresAt(normalExpiresAt);
            showSeatInventoryRepository.save(row);
        }

        // 8. Transition Booking: INITIATED -> HELD
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.HELD);
        booking = bookingRepository.save(booking);

        // 9. Record Idempotency
        BookingIdempotencyRecord record = existingRecordOpt.orElseGet(() -> BookingIdempotencyRecord.builder()
                .userId(userId)
                .operationType("CREATE_HOLD")
                .idempotencyKey(idempotencyKey)
                .build());
        record.setRequestHash(requestHash);
        record.setBookingId(booking.getBookingId());
        record.setStatus(IdempotencyStatus.COMPLETED);
        idempotencyRecordRepository.save(record);

        return SeatHoldResponse.builder()
                .holdId(hold.getHoldId())
                .bookingId(booking.getBookingId())
                .showId(showId)
                .status(hold.getStatus().name())
                .normalExpiresAt(hold.getNormalExpiresAt())
                .paymentGraceExpiresAt(hold.getPaymentGraceExpiresAt())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .build();
    }

    private SeatHoldResponse replaySeatHoldResponse(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for replay: " + bookingId));
        SeatHold hold = seatHoldRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found for replay: " + bookingId));

        return SeatHoldResponse.builder()
                .holdId(hold.getHoldId())
                .bookingId(booking.getBookingId())
                .showId(booking.getShowId())
                .status(hold.getStatus().name())
                .normalExpiresAt(hold.getNormalExpiresAt())
                .paymentGraceExpiresAt(hold.getPaymentGraceExpiresAt())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .build();
    }

    /**
     * 3. GET /api/v1/holds/{holdId}
     */
    @Transactional
    public SeatHoldDto getHoldById(UUID holdId, UUID userId, boolean isPrivileged) {
        SeatHold hold = seatHoldRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found: " + holdId));

        if (!isPrivileged && !hold.getUserId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to view this hold");
        }

        // Lazy expiry check
        OffsetDateTime now = OffsetDateTime.now();
        if (hold.getStatus() == HoldStatus.ACTIVE) {
            boolean isExpired = (hold.getPaymentGraceExpiresAt() != null)
                    ? hold.getPaymentGraceExpiresAt().isBefore(now)
                    : hold.getNormalExpiresAt().isBefore(now);

            if (isExpired) {
                hold.setStatus(HoldStatus.EXPIRED);
                hold = seatHoldRepository.save(hold);
            }
        }

        return SeatHoldDto.builder()
                .holdId(hold.getHoldId())
                .bookingId(hold.getBookingId())
                .showId(hold.getShowId())
                .status(hold.getStatus().name())
                .normalExpiresAt(hold.getNormalExpiresAt())
                .paymentGraceExpiresAt(hold.getPaymentGraceExpiresAt())
                .build();
    }

    /**
     * 4. DELETE /api/v1/holds/{holdId}
     * Release unpaid active hold before expiry.
     */
    @Transactional
    public void releaseHold(UUID holdId, UUID userId, boolean isPrivileged) {
        SeatHold hold = seatHoldRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found: " + holdId));

        if (!isPrivileged && !hold.getUserId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to release this hold");
        }

        Booking booking = bookingRepository.findById(hold.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + hold.getBookingId()));

        if (booking.getStatus() == BookingStatus.PAYMENT_PENDING
                || booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.TICKET_ISSUED) {
            throw new ConflictException("Cannot release hold in status: " + booking.getStatus());
        }

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new ConflictException("Hold is not ACTIVE: " + hold.getStatus());
        }

        // Update hold and booking
        hold.setStatus(HoldStatus.RELEASED);
        seatHoldRepository.save(hold);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Release inventory back to AVAILABLE
        List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByActiveHoldId(holdId);
        for (ShowSeatInventory item : inventory) {
            item.setStatus(InventoryStatus.AVAILABLE);
            item.setActiveHoldId(null);
            item.setHoldExpiresAt(null);
            showSeatInventoryRepository.save(item);
        }
    }

    /**
     * 5. GET /api/v1/bookings/me
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(UUID userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return bookings.stream().map(this::mapToBookingResponse).collect(Collectors.toList());
    }

    /**
     * 6. GET /api/v1/bookings/{bookingId}
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId, UUID userId, boolean isPrivileged) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!isPrivileged && !booking.getUserId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to view this booking");
        }

        return mapToBookingResponse(booking);
    }

    /**
     * 7. POST /api/v1/bookings/{bookingId}/cancel
     * Cancels confirmed booking if policy permits.
     */
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID userId, boolean isPrivileged) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!isPrivileged && !booking.getUserId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.TICKET_ISSUED) {
            throw new ConflictException("Only CONFIRMED or TICKET_ISSUED bookings can be cancelled. Current status: " + booking.getStatus());
        }

        // Evaluate Show Service cancellation policy
        CancellationPolicyDto policy = showServiceClient.getCancellationPolicy(booking.getShowId())
                .orElseThrow(() -> new UnprocessableEntityException("No cancellation policy found for show"));

        if ("NON_CANCELLABLE".equalsIgnoreCase(policy.getPolicyType())) {
            throw new UnprocessableEntityException("Show cancellation policy is NON_CANCELLABLE");
        }

        if (policy.getCancellationDeadline() != null && OffsetDateTime.now().isAfter(policy.getCancellationDeadline())) {
            throw new UnprocessableEntityException("Cancellation deadline has passed: " + policy.getCancellationDeadline());
        }

        // Transition booking to CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        // Release inventory back to AVAILABLE
        List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByCurrentBookingId(bookingId);
        for (ShowSeatInventory item : inventory) {
            item.setStatus(InventoryStatus.AVAILABLE);
            item.setCurrentBookingId(null);
            showSeatInventoryRepository.save(item);
        }

        // Invalidate ticket
        ticketRepository.findByBookingId(bookingId).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        });

        // Trigger Payment Service refund if paymentId exists
        if (booking.getPaymentId() != null) {
            paymentServiceClient.initiateRefund(booking.getPaymentId(), bookingId, "Customer cancelled booking");
        }

        return mapToBookingResponse(booking);
    }

    /**
     * 8. GET /api/v1/bookings/{bookingId}/ticket
     * Accessible ONLY when booking is CONFIRMED or TICKET_ISSUED.
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicketByBooking(UUID bookingId, UUID userId, boolean isPrivileged) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!isPrivileged && !booking.getUserId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to view this ticket");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.TICKET_ISSUED) {
            throw new ConflictException("Ticket is only available for CONFIRMED or TICKET_ISSUED bookings. Current status: " + booking.getStatus());
        }

        Ticket ticket = ticketRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found for booking: " + bookingId));

        Object snapshotObj = null;
        try {
            snapshotObj = objectMapper.readValue(ticket.getHistoricalSnapshot(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            snapshotObj = ticket.getHistoricalSnapshot();
        }

        return TicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .bookingId(ticket.getBookingId())
                .ticketCode(ticket.getTicketCode())
                .status(ticket.getStatus())
                .historicalSnapshot(snapshotObj)
                .issuedAt(ticket.getIssuedAt())
                .build();
    }

    /**
     * 9. POST /internal/v1/bookings/{bookingId}/apply-payment-pending
     * Called by Payment Service to apply 2-minute payment grace.
     */
    @Transactional
    public ApplyPaymentPendingInternalResponse applyPaymentPending(UUID bookingId, ApplyPaymentPendingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUserId().equals(request.getUserId())) {
            throw new ConflictException("Booking user mismatch");
        }

        if (booking.getStatus() != BookingStatus.HELD && booking.getStatus() != BookingStatus.INITIATED) {
            throw new ConflictException("Booking is not in a payable hold state: " + booking.getStatus());
        }

        SeatHold hold = seatHoldRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat hold not found for booking: " + bookingId));

        OffsetDateTime now = OffsetDateTime.now();
        if (hold.getStatus() != HoldStatus.ACTIVE || hold.getNormalExpiresAt().isBefore(now)) {
            throw new ConflictException("Seat hold has expired. Normal expiry was: " + hold.getNormalExpiresAt());
        }

        // Apply bounded 2-minute payment grace period
        OffsetDateTime graceExpiresAt = hold.getNormalExpiresAt().plusMinutes(2);
        hold.setPaymentGraceExpiresAt(graceExpiresAt);
        seatHoldRepository.save(hold);

        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        booking.setPaymentGraceExpiresAt(graceExpiresAt);
        booking = bookingRepository.save(booking);

        // Update inventory expiry to grace expiry
        List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByActiveHoldId(hold.getHoldId());
        for (ShowSeatInventory item : inventory) {
            item.setHoldExpiresAt(graceExpiresAt);
            showSeatInventoryRepository.save(item);
        }

        return ApplyPaymentPendingInternalResponse.builder()
                .bookingId(booking.getBookingId())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(booking.getStatus())
                .paymentGraceExpiresAt(booking.getPaymentGraceExpiresAt())
                .build();
    }

    /**
     * 10. POST /internal/v1/bookings/{bookingId}/payment-outcome
     * Processes payment outcome from Payment Service.
     */
    @Transactional
    public PaymentOutcomeInternalResponse processPaymentOutcome(UUID bookingId, PaymentOutcomeRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        Optional<SeatHold> holdOpt = seatHoldRepository.findByBookingId(bookingId);
        String outcome = request.getOutcome() != null ? request.getOutcome().toUpperCase() : "";
        OffsetDateTime now = OffsetDateTime.now();

        switch (outcome) {
            case "SUCCESS":
                // Check if grace expired
                boolean expired = booking.getPaymentGraceExpiresAt() != null && booking.getPaymentGraceExpiresAt().isBefore(now);
                if (expired) {
                    log.warn("Payment succeeded but hold grace expired for booking {}", bookingId);
                    booking.setStatus(BookingStatus.REFUND_PENDING);
                    booking.setPaymentId(request.getPaymentId());
                    bookingRepository.save(booking);

                    holdOpt.ifPresent(h -> {
                        h.setStatus(HoldStatus.EXPIRED);
                        seatHoldRepository.save(h);
                        releaseHeldInventory(h.getHoldId());
                    });

                    paymentServiceClient.initiateRefund(request.getPaymentId(), bookingId,
                            "Payment completed after grace period expired");

                    return PaymentOutcomeInternalResponse.builder()
                            .bookingId(bookingId)
                            .bookingStatus(BookingStatus.REFUND_PENDING)
                            .ticketIssued(false)
                            .build();
                }

                // Normal SUCCESS flow
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setPaymentId(request.getPaymentId());
                bookingRepository.save(booking);

                if (holdOpt.isPresent()) {
                    SeatHold h = holdOpt.get();
                    h.setStatus(HoldStatus.CONVERTED);
                    seatHoldRepository.save(h);

                    List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByActiveHoldId(h.getHoldId());
                    for (ShowSeatInventory item : inventory) {
                        item.setStatus(InventoryStatus.BOOKED);
                        item.setActiveHoldId(null);
                        item.setCurrentBookingId(bookingId);
                        item.setHoldExpiresAt(null);
                        showSeatInventoryRepository.save(item);
                    }
                }

                // Generate digital Ticket
                String ticketCode = "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
                String snapshotJson = buildTicketSnapshot(booking);

                Ticket ticket = Ticket.builder()
                        .bookingId(bookingId)
                        .ticketCode(ticketCode)
                        .status(TicketStatus.ISSUED)
                        .historicalSnapshot(snapshotJson)
                        .issuedAt(now)
                        .build();
                ticketRepository.save(ticket);

                booking.setStatus(BookingStatus.TICKET_ISSUED);
                booking = bookingRepository.save(booking);

                return PaymentOutcomeInternalResponse.builder()
                        .bookingId(bookingId)
                        .bookingStatus(BookingStatus.TICKET_ISSUED)
                        .ticketIssued(true)
                        .build();

            case "FAILURE":
                booking.setStatus(BookingStatus.PAYMENT_FAILED);
                booking.setPaymentId(request.getPaymentId());
                bookingRepository.save(booking);

                holdOpt.ifPresent(h -> {
                    h.setStatus(HoldStatus.RELEASED);
                    seatHoldRepository.save(h);
                    releaseHeldInventory(h.getHoldId());
                });

                return PaymentOutcomeInternalResponse.builder()
                        .bookingId(bookingId)
                        .bookingStatus(BookingStatus.PAYMENT_FAILED)
                        .ticketIssued(false)
                        .build();

            case "CANCELLED":
                booking.setStatus(BookingStatus.PAYMENT_CANCELLED);
                booking.setPaymentId(request.getPaymentId());
                bookingRepository.save(booking);

                holdOpt.ifPresent(h -> {
                    h.setStatus(HoldStatus.RELEASED);
                    seatHoldRepository.save(h);
                    releaseHeldInventory(h.getHoldId());
                });

                return PaymentOutcomeInternalResponse.builder()
                        .bookingId(bookingId)
                        .bookingStatus(BookingStatus.PAYMENT_CANCELLED)
                        .ticketIssued(false)
                        .build();

            case "UNKNOWN":
                booking.setStatus(BookingStatus.PAYMENT_UNKNOWN);
                booking.setPaymentId(request.getPaymentId());
                bookingRepository.save(booking);

                return PaymentOutcomeInternalResponse.builder()
                        .bookingId(bookingId)
                        .bookingStatus(BookingStatus.PAYMENT_UNKNOWN)
                        .ticketIssued(false)
                        .build();

            default:
                throw new BadRequestException("Invalid payment outcome: " + request.getOutcome());
        }
    }

    private void releaseHeldInventory(UUID holdId) {
        List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByActiveHoldId(holdId);
        for (ShowSeatInventory item : inventory) {
            item.setStatus(InventoryStatus.AVAILABLE);
            item.setActiveHoldId(null);
            item.setHoldExpiresAt(null);
            showSeatInventoryRepository.save(item);
        }
    }

    private String buildTicketSnapshot(Booking booking) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("bookingId", booking.getBookingId().toString());
        snapshot.put("userId", booking.getUserId().toString());
        snapshot.put("showId", booking.getShowId().toString());
        snapshot.put("totalAmount", booking.getTotalAmount());
        snapshot.put("currency", booking.getCurrency());
        snapshot.put("createdAt", booking.getCreatedAt().toString());

        List<BookingSeat> seats = bookingSeatRepository.findByBookingId(booking.getBookingId());
        List<Map<String, Object>> seatList = seats.stream().map(s -> {
            Map<String, Object> sm = new HashMap<>();
            sm.put("physicalSeatId", s.getPhysicalSeatId().toString());
            sm.put("seatLabel", s.getSeatLabelSnapshot());
            sm.put("seatCategory", s.getSeatCategorySnapshot());
            sm.put("unitPrice", s.getUnitPriceSnapshot());
            return sm;
        }).collect(Collectors.toList());
        snapshot.put("seats", seatList);

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "{}";
        }
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        List<BookingSeat> seats = bookingSeatRepository.findByBookingId(booking.getBookingId());
        List<BookingSeatResponse> seatResponses = seats.stream().map(s ->
                BookingSeatResponse.builder()
                        .bookingSeatId(s.getBookingSeatId())
                        .physicalSeatId(s.getPhysicalSeatId())
                        .seatLabelSnapshot(s.getSeatLabelSnapshot())
                        .seatCategorySnapshot(s.getSeatCategorySnapshot())
                        .unitPriceSnapshot(s.getUnitPriceSnapshot())
                        .build()
        ).collect(Collectors.toList());

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(booking.getStatus())
                .paymentGraceExpiresAt(booking.getPaymentGraceExpiresAt())
                .paymentId(booking.getPaymentId())
                .bookingSeats(seatResponses)
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
