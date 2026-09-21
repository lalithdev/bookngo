package com.bookngo.bookingservice;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.bookngo.bookingservice.client.PaymentServiceClient;
import com.bookngo.bookingservice.client.ShowServiceClient;
import com.bookngo.bookingservice.client.TheatreServiceClient;
import com.bookngo.bookingservice.dto.PhysicalSeatDto;
import com.bookngo.bookingservice.dto.SeatHoldRequest;
import com.bookngo.bookingservice.dto.SeatHoldResponse;
import com.bookngo.bookingservice.dto.ShowDto;
import com.bookngo.bookingservice.dto.ShowPricingDto;
import com.bookngo.bookingservice.entity.HoldStatus;
import com.bookngo.bookingservice.entity.InventoryStatus;
import com.bookngo.bookingservice.entity.SeatHold;
import com.bookngo.bookingservice.entity.ShowSeatInventory;
import com.bookngo.bookingservice.exception.ConflictException;
import com.bookngo.bookingservice.repository.BookingIdempotencyRecordRepository;
import com.bookngo.bookingservice.repository.BookingRepository;
import com.bookngo.bookingservice.repository.BookingSeatRepository;
import com.bookngo.bookingservice.repository.SeatHoldRepository;
import com.bookngo.bookingservice.repository.ShowSeatInventoryRepository;
import com.bookngo.bookingservice.repository.TicketRepository;
import com.bookngo.bookingservice.service.BookingService;

@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyIntegrationTest {

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

    @MockitoBean
    private TheatreServiceClient theatreServiceClient;

    @MockitoBean
    private ShowServiceClient showServiceClient;

    @MockitoBean
    private PaymentServiceClient paymentServiceClient;

    private UUID showId;
    private UUID screenId;
    private UUID seatA;
    private UUID seatB;
    private UUID seatC;

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
        seatA = UUID.randomUUID();
        seatB = UUID.randomUUID();
        seatC = UUID.randomUUID();

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

        ShowPricingDto pricing = ShowPricingDto.builder()
                .showPricingId(UUID.randomUUID())
                .showId(showId)
                .seatCategory("REGULAR")
                .amount(BigDecimal.valueOf(150.00))
                .currency("INR")
                .status("ACTIVE")
                .build();
        when(showServiceClient.getShowPricing(showId)).thenReturn(List.of(pricing));

        PhysicalSeatDto pSeatA = PhysicalSeatDto.builder().physicalSeatId(seatA).screenId(screenId).rowLabel("A").seatNumber("1").seatCategory("REGULAR").status("ACTIVE").build();
        PhysicalSeatDto pSeatB = PhysicalSeatDto.builder().physicalSeatId(seatB).screenId(screenId).rowLabel("A").seatNumber("2").seatCategory("REGULAR").status("ACTIVE").build();
        PhysicalSeatDto pSeatC = PhysicalSeatDto.builder().physicalSeatId(seatC).screenId(screenId).rowLabel("A").seatNumber("3").seatCategory("REGULAR").status("ACTIVE").build();
        when(theatreServiceClient.getPhysicalSeatsByScreen(screenId)).thenReturn(List.of(pSeatA, pSeatB, pSeatC));

        // Pre-initialize inventory
        bookingService.ensureInventoryInitialized(showId);
    }

    @Test
    void testConcurrentHoldSameSeatExactOneWinner() throws InterruptedException {
        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<SeatHoldResponse> successfulResponses = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            UUID userId = UUID.randomUUID();
            executor.submit(() -> {
                try {
                    startGate.await(); // wait for simultaneous launch
                    SeatHoldRequest request = SeatHoldRequest.builder()
                            .physicalSeatIds(List.of(seatA))
                            .build();
                    SeatHoldResponse response = bookingService.createSeatHold(
                            showId, userId, UUID.randomUUID().toString(), request);
                    successCount.incrementAndGet();
                    successfulResponses.add(response);
                } catch (ConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startGate.countDown();
        boolean completed = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent requests should finish within timeout");
        assertEquals(1, successCount.get(), "Exactly ONE concurrent allocation must succeed for seatA");
        assertEquals(numberOfThreads - 1, conflictCount.get(), "All other requests must receive ConflictException (409)");

        // Database-level verification
        List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByShowId(showId);
        ShowSeatInventory seatARow = inventory.stream()
                .filter(s -> s.getPhysicalSeatId().equals(seatA))
                .findFirst().orElseThrow();

        assertEquals(InventoryStatus.HELD, seatARow.getStatus());
        assertEquals(successfulResponses.get(0).getHoldId(), seatARow.getActiveHoldId());

        // Verify seat holds table has exactly 1 active hold for this show
        List<SeatHold> activeHolds = seatHoldRepository.findAll().stream()
                .filter(h -> h.getStatus() == HoldStatus.ACTIVE)
                .toList();
        assertEquals(1, activeHolds.size(), "Zero double bookings: exactly 1 active hold exists in database");
    }

    @Test
    void testConcurrentOverlappingMultiSeatRequests() throws InterruptedException {
        // 3 competing groups:
        // Group 1: requests {seatA, seatB}
        // Group 2: requests {seatB, seatC}
        // Group 3: requests {seatA, seatC}
        int totalClients = 30;
        ExecutorService executor = Executors.newFixedThreadPool(totalClients);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalClients);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < totalClients; i++) {
            final int group = i % 3;
            final List<UUID> requestedSeats = switch (group) {
                case 0 -> List.of(seatA, seatB);
                case 1 -> List.of(seatB, seatC);
                default -> List.of(seatA, seatC);
            };
            UUID userId = UUID.randomUUID();

            executor.submit(() -> {
                try {
                    startGate.await();
                    SeatHoldRequest request = SeatHoldRequest.builder()
                            .physicalSeatIds(requestedSeats)
                            .build();
                    bookingService.createSeatHold(showId, userId, UUID.randomUUID().toString(), request);
                    successCount.incrementAndGet();
                } catch (ConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed);
        assertEquals(1, successCount.get(), "Only 1 group can acquire overlapping pair without conflict");
        assertEquals(totalClients - 1, conflictCount.get());

        // Invariant check across database
        List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByShowId(showId);
        long heldCount = inventory.stream().filter(s -> s.getStatus() == InventoryStatus.HELD).count();
        assertEquals(2, heldCount, "Exactly 2 seats held by the single winning pair");

        long availableCount = inventory.stream().filter(s -> s.getStatus() == InventoryStatus.AVAILABLE).count();
        assertEquals(1, availableCount, "Remaining non-held seat remains AVAILABLE with no partial allocation leak");
    }
}
