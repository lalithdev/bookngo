package com.bookngo.bookingservice;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.bookngo.bookingservice.client.PaymentServiceClient;
import com.bookngo.bookingservice.client.ShowServiceClient;
import com.bookngo.bookingservice.client.TheatreServiceClient;
import com.bookngo.bookingservice.dto.ApplyPaymentPendingInternalResponse;
import com.bookngo.bookingservice.dto.ApplyPaymentPendingRequest;
import com.bookngo.bookingservice.dto.BookingResponse;
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
import com.bookngo.bookingservice.entity.BookingStatus;
import com.bookngo.bookingservice.entity.HoldStatus;
import com.bookngo.bookingservice.entity.InventoryStatus;
import com.bookngo.bookingservice.entity.SeatHold;
import com.bookngo.bookingservice.entity.ShowSeatInventory;
import com.bookngo.bookingservice.entity.Ticket;
import com.bookngo.bookingservice.entity.TicketStatus;
import com.bookngo.bookingservice.exception.BadRequestException;
import com.bookngo.bookingservice.exception.ConflictException;
import com.bookngo.bookingservice.exception.ForbiddenException;
import com.bookngo.bookingservice.exception.UnprocessableEntityException;
import com.bookngo.bookingservice.repository.BookingIdempotencyRecordRepository;
import com.bookngo.bookingservice.repository.BookingRepository;
import com.bookngo.bookingservice.repository.BookingSeatRepository;
import com.bookngo.bookingservice.repository.SeatHoldRepository;
import com.bookngo.bookingservice.repository.ShowSeatInventoryRepository;
import com.bookngo.bookingservice.repository.TicketRepository;
import com.bookngo.bookingservice.security.JwtService;
import com.bookngo.bookingservice.service.BookingService;

@SpringBootTest
@ActiveProfiles("test")
class BookingServiceWorkflowTests {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private ShowSeatInventoryRepository showSeatInventoryRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingIdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private TheatreServiceClient theatreServiceClient;

    @MockitoBean
    private ShowServiceClient showServiceClient;

    @MockitoBean
    private PaymentServiceClient paymentServiceClient;

    private UUID showId;
    private UUID screenId;
    private UUID customerId;
    private UUID seat1Id;
    private UUID seat2Id;
    private UUID seat3Id;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        showSeatInventoryRepository.deleteAll();
        seatHoldRepository.deleteAll();
        bookingRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();

        showId = UUID.randomUUID();
        screenId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        seat1Id = UUID.randomUUID();
        seat2Id = UUID.randomUUID();
        seat3Id = UUID.randomUUID();

        // Mock Show Service
        ShowDto show = ShowDto.builder()
                .showId(showId)
                .movieId(UUID.randomUUID())
                .theatreId(UUID.randomUUID())
                .screenId(screenId)
                .startsAt(OffsetDateTime.now().plusDays(1))
                .endsAt(OffsetDateTime.now().plusDays(1).plusHours(2))
                .showFormat("2D")
                .status("SCHEDULED")
                .build();
        when(showServiceClient.getShow(showId)).thenReturn(Optional.of(show));

        // Mock Pricing
        ShowPricingDto pricing = ShowPricingDto.builder()
                .showPricingId(UUID.randomUUID())
                .showId(showId)
                .seatCategory("REGULAR")
                .amount(BigDecimal.valueOf(200.00))
                .currency("INR")
                .status("ACTIVE")
                .build();
        when(showServiceClient.getShowPricing(showId)).thenReturn(List.of(pricing));

        // Mock Physical Seats in Theatre Service
        PhysicalSeatDto seat1 = PhysicalSeatDto.builder()
                .physicalSeatId(seat1Id)
                .screenId(screenId)
                .rowLabel("A")
                .seatNumber("1")
                .seatCategory("REGULAR")
                .status("ACTIVE")
                .build();
        PhysicalSeatDto seat2 = PhysicalSeatDto.builder()
                .physicalSeatId(seat2Id)
                .screenId(screenId)
                .rowLabel("A")
                .seatNumber("2")
                .seatCategory("REGULAR")
                .status("ACTIVE")
                .build();
        PhysicalSeatDto seat3 = PhysicalSeatDto.builder()
                .physicalSeatId(seat3Id)
                .screenId(screenId)
                .rowLabel("A")
                .seatNumber("3")
                .seatCategory("REGULAR")
                .status("ACTIVE")
                .build();
        when(theatreServiceClient.getPhysicalSeatsByScreen(screenId)).thenReturn(List.of(seat1, seat2, seat3));
    }

    @Test
    void testSeatInventoryInitializationAndAvailability() {
        List<SeatInventoryItem> items = bookingService.getShowSeatInventory(showId);
        assertEquals(3, items.size());
        assertTrue(items.stream().allMatch(i -> "AVAILABLE".equals(i.getStatus())));
    }

    @Test
    void testCreateSeatHoldSuccess() {
        SeatHoldRequest request = SeatHoldRequest.builder()
                .physicalSeatIds(List.of(seat1Id, seat2Id))
                .build();

        String idempotencyKey = UUID.randomUUID().toString();
        SeatHoldResponse response = bookingService.createSeatHold(showId, customerId, idempotencyKey, request);

        assertNotNull(response.getHoldId());
        assertNotNull(response.getBookingId());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(BigDecimal.valueOf(400.00), response.getTotalAmount());
        assertEquals("INR", response.getCurrency());

        // Verify booking state is HELD
        Booking booking = bookingRepository.findById(response.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.HELD, booking.getStatus());

        // Verify seat hold
        SeatHold hold = seatHoldRepository.findById(response.getHoldId()).orElseThrow();
        assertEquals(HoldStatus.ACTIVE, hold.getStatus());

        // Verify inventory is HELD
        List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByShowId(showId);
        long heldCount = inventory.stream().filter(s -> s.getStatus() == InventoryStatus.HELD).count();
        assertEquals(2, heldCount);
    }

    @Test
    void testMaximumSeatsValidation() {
        List<UUID> tooManySeats = List.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(tooManySeats).build();

        assertThrows(BadRequestException.class, () ->
                bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request));
    }

    @Test
    void testEmptySeatsValidation() {
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(Collections.emptyList()).build();

        assertThrows(BadRequestException.class, () ->
                bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request));
    }

    @Test
    void testDuplicateSeatsInRequestValidation() {
        SeatHoldRequest request = SeatHoldRequest.builder()
                .physicalSeatIds(List.of(seat1Id, seat1Id))
                .build();

        assertThrows(BadRequestException.class, () ->
                bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request));
    }

    @Test
    void testAlreadyHeldSeatConflict() {
        SeatHoldRequest request1 = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id)).build();
        bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request1);

        UUID customer2 = UUID.randomUUID();
        SeatHoldRequest request2 = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id, seat2Id)).build();

        assertThrows(ConflictException.class, () ->
                bookingService.createSeatHold(showId, customer2, UUID.randomUUID().toString(), request2));

        // Ensure seat2 was NOT partially allocated!
        List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByShowId(showId);
        ShowSeatInventory seat2Row = inventory.stream()
                .filter(s -> s.getPhysicalSeatId().equals(seat2Id))
                .findFirst().orElseThrow();
        assertEquals(InventoryStatus.AVAILABLE, seat2Row.getStatus());
    }

    @Test
    void testIdempotencyReplayAndConflict() {
        String key = UUID.randomUUID().toString();
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id)).build();

        // First call
        SeatHoldResponse response1 = bookingService.createSeatHold(showId, customerId, key, request);

        // Same key, same request -> Replay original response!
        SeatHoldResponse response2 = bookingService.createSeatHold(showId, customerId, key, request);
        assertEquals(response1.getHoldId(), response2.getHoldId());
        assertEquals(response1.getBookingId(), response2.getBookingId());

        // Same key, different request -> 409 Conflict!
        SeatHoldRequest differentRequest = SeatHoldRequest.builder().physicalSeatIds(List.of(seat2Id)).build();
        assertThrows(ConflictException.class, () ->
                bookingService.createSeatHold(showId, customerId, key, differentRequest));
    }

    @Test
    void testHoldStatusAndOwnership() {
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id)).build();
        SeatHoldResponse response = bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request);

        // Owner access -> 200 OK
        SeatHoldDto hold = bookingService.getHoldById(response.getHoldId(), customerId, false);
        assertEquals("ACTIVE", hold.getStatus());

        // Unauthorized user access -> 403 Forbidden
        UUID otherUser = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () ->
                bookingService.getHoldById(response.getHoldId(), otherUser, false));

        // Privileged user access -> 200 OK
        SeatHoldDto privilegedHold = bookingService.getHoldById(response.getHoldId(), otherUser, true);
        assertNotNull(privilegedHold);
    }

    @Test
    void testReleaseHoldSuccess() {
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id)).build();
        SeatHoldResponse response = bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request);

        bookingService.releaseHold(response.getHoldId(), customerId, false);

        SeatHold hold = seatHoldRepository.findById(response.getHoldId()).orElseThrow();
        assertEquals(HoldStatus.RELEASED, hold.getStatus());

        Booking booking = bookingRepository.findById(response.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());

        ShowSeatInventory seat = showSeatInventoryRepository.findByShowId(showId).stream()
                .filter(s -> s.getPhysicalSeatId().equals(seat1Id))
                .findFirst().orElseThrow();
        assertEquals(InventoryStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void testPaymentPendingGraceAndOutcomeFlow() {
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id)).build();
        SeatHoldResponse holdResponse = bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request);

        // 1. Payment Service calls apply-payment-pending
        ApplyPaymentPendingRequest applyRequest = ApplyPaymentPendingRequest.builder().userId(customerId).build();
        ApplyPaymentPendingInternalResponse pendingResponse =
                bookingService.applyPaymentPending(holdResponse.getBookingId(), applyRequest);

        assertEquals(BookingStatus.PAYMENT_PENDING, pendingResponse.getStatus());
        assertNotNull(pendingResponse.getPaymentGraceExpiresAt());

        // 2. Payment Service calls payment-outcome with SUCCESS
        UUID paymentId = UUID.randomUUID();
        PaymentOutcomeRequest outcomeRequest = PaymentOutcomeRequest.builder()
                .paymentId(paymentId)
                .outcome("SUCCESS")
                .build();

        PaymentOutcomeInternalResponse outcomeResponse =
                bookingService.processPaymentOutcome(holdResponse.getBookingId(), outcomeRequest);

        assertEquals(BookingStatus.TICKET_ISSUED, outcomeResponse.getBookingStatus());
        assertTrue(outcomeResponse.isTicketIssued());

        // Verify Ticket exists
        Ticket ticket = ticketRepository.findByBookingId(holdResponse.getBookingId()).orElseThrow();
        assertEquals(TicketStatus.ISSUED, ticket.getStatus());
        assertNotNull(ticket.getTicketCode());

        // Verify seats are BOOKED
        ShowSeatInventory seat = showSeatInventoryRepository.findByShowId(showId).stream()
                .filter(s -> s.getPhysicalSeatId().equals(seat1Id))
                .findFirst().orElseThrow();
        assertEquals(InventoryStatus.BOOKED, seat.getStatus());
        assertEquals(holdResponse.getBookingId(), seat.getCurrentBookingId());
    }

    @Test
    void testPaymentFailureReleasesSeats() {
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id)).build();
        SeatHoldResponse holdResponse = bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request);

        bookingService.applyPaymentPending(holdResponse.getBookingId(),
                ApplyPaymentPendingRequest.builder().userId(customerId).build());

        PaymentOutcomeRequest outcomeRequest = PaymentOutcomeRequest.builder()
                .paymentId(UUID.randomUUID())
                .outcome("FAILURE")
                .build();

        PaymentOutcomeInternalResponse outcomeResponse =
                bookingService.processPaymentOutcome(holdResponse.getBookingId(), outcomeRequest);

        assertEquals(BookingStatus.PAYMENT_FAILED, outcomeResponse.getBookingStatus());
        assertFalse(outcomeResponse.isTicketIssued());

        // Seats returned to AVAILABLE
        ShowSeatInventory seat = showSeatInventoryRepository.findByShowId(showId).stream()
                .filter(s -> s.getPhysicalSeatId().equals(seat1Id))
                .findFirst().orElseThrow();
        assertEquals(InventoryStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void testTicketAccessRequiresConfirmation() {
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id)).build();
        SeatHoldResponse holdResponse = bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request);

        // While HELD, ticket request must fail with 409 Conflict!
        assertThrows(ConflictException.class, () ->
                bookingService.getTicketByBooking(holdResponse.getBookingId(), customerId, false));

        // Progress to CONFIRMED / TICKET_ISSUED
        bookingService.applyPaymentPending(holdResponse.getBookingId(),
                ApplyPaymentPendingRequest.builder().userId(customerId).build());
        bookingService.processPaymentOutcome(holdResponse.getBookingId(),
                PaymentOutcomeRequest.builder().paymentId(UUID.randomUUID()).outcome("SUCCESS").build());

        // Ticket is now accessible
        TicketResponse ticket = bookingService.getTicketByBooking(holdResponse.getBookingId(), customerId, false);
        assertNotNull(ticket);
        assertEquals(TicketStatus.ISSUED, ticket.getStatus());
    }

    @Test
    void testConfirmedBookingCancellationWithPolicy() {
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id)).build();
        SeatHoldResponse holdResponse = bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request);

        bookingService.applyPaymentPending(holdResponse.getBookingId(),
                ApplyPaymentPendingRequest.builder().userId(customerId).build());
        bookingService.processPaymentOutcome(holdResponse.getBookingId(),
                PaymentOutcomeRequest.builder().paymentId(UUID.randomUUID()).outcome("SUCCESS").build());

        // Mock Cancellable Policy within deadline
        CancellationPolicyDto policy = CancellationPolicyDto.builder()
                .cancellationPolicyId(UUID.randomUUID())
                .showId(showId)
                .policyType("CANCELLABLE")
                .cancellationDeadline(OffsetDateTime.now().plusHours(12))
                .status("ACTIVE")
                .build();
        when(showServiceClient.getCancellationPolicy(showId)).thenReturn(Optional.of(policy));

        BookingResponse cancelled = bookingService.cancelBooking(holdResponse.getBookingId(), customerId, false);
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());

        // Inventory returned to AVAILABLE
        ShowSeatInventory seat = showSeatInventoryRepository.findByShowId(showId).stream()
                .filter(s -> s.getPhysicalSeatId().equals(seat1Id))
                .findFirst().orElseThrow();
        assertEquals(InventoryStatus.AVAILABLE, seat.getStatus());

        // Ticket status CANCELLED
        Ticket ticket = ticketRepository.findByBookingId(holdResponse.getBookingId()).orElseThrow();
        assertEquals(TicketStatus.CANCELLED, ticket.getStatus());
    }

    @Test
    void testNonCancellablePolicyFailsCancellation() {
        SeatHoldRequest request = SeatHoldRequest.builder().physicalSeatIds(List.of(seat1Id)).build();
        SeatHoldResponse holdResponse = bookingService.createSeatHold(showId, customerId, UUID.randomUUID().toString(), request);

        bookingService.applyPaymentPending(holdResponse.getBookingId(),
                ApplyPaymentPendingRequest.builder().userId(customerId).build());
        bookingService.processPaymentOutcome(holdResponse.getBookingId(),
                PaymentOutcomeRequest.builder().paymentId(UUID.randomUUID()).outcome("SUCCESS").build());

        CancellationPolicyDto policy = CancellationPolicyDto.builder()
                .cancellationPolicyId(UUID.randomUUID())
                .showId(showId)
                .policyType("NON_CANCELLABLE")
                .status("ACTIVE")
                .build();
        when(showServiceClient.getCancellationPolicy(showId)).thenReturn(Optional.of(policy));

        assertThrows(UnprocessableEntityException.class, () ->
                bookingService.cancelBooking(holdResponse.getBookingId(), customerId, false));
    }
}
