package com.bookngo.bookingservice;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.bookingservice.client.PaymentServiceClient;
import com.bookngo.bookingservice.client.ShowServiceClient;
import com.bookngo.bookingservice.client.TheatreServiceClient;
import com.bookngo.bookingservice.dto.PhysicalSeatDto;
import com.bookngo.bookingservice.dto.ShowDto;
import com.bookngo.bookingservice.dto.ShowPricingDto;
import com.bookngo.bookingservice.entity.Booking;
import com.bookngo.bookingservice.entity.BookingStatus;
import com.bookngo.bookingservice.entity.HoldStatus;
import com.bookngo.bookingservice.entity.SeatHold;
import com.bookngo.bookingservice.repository.BookingIdempotencyRecordRepository;
import com.bookngo.bookingservice.repository.BookingRepository;
import com.bookngo.bookingservice.repository.BookingSeatRepository;
import com.bookngo.bookingservice.repository.SeatHoldRepository;
import com.bookngo.bookingservice.repository.ShowSeatInventoryRepository;
import com.bookngo.bookingservice.repository.TicketRepository;
import com.bookngo.bookingservice.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

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

    private UUID customerId;
    private UUID showId;
    private String customerToken;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        showSeatInventoryRepository.deleteAll();
        seatHoldRepository.deleteAll();
        bookingRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();

        customerId = UUID.randomUUID();
        showId = UUID.randomUUID();
        customerToken = jwtService.generateToken(customerId, List.of("CUSTOMER"));

        ShowDto show = ShowDto.builder()
                .showId(showId)
                .movieId(UUID.randomUUID())
                .theatreId(UUID.randomUUID())
                .screenId(UUID.randomUUID())
                .startsAt(OffsetDateTime.now().plusDays(1))
                .endsAt(OffsetDateTime.now().plusDays(1).plusHours(2))
                .showFormat("2D")
                .status("SCHEDULED")
                .build();
        when(showServiceClient.getShow(showId)).thenReturn(Optional.of(show));
        when(showServiceClient.getShowPricing(showId)).thenReturn(List.of(
                ShowPricingDto.builder().showPricingId(UUID.randomUUID()).showId(showId).seatCategory("REGULAR").amount(BigDecimal.valueOf(150.00)).currency("INR").status("ACTIVE").build()
        ));
        when(theatreServiceClient.getPhysicalSeatsByScreen(any())).thenReturn(List.of(
                PhysicalSeatDto.builder().physicalSeatId(UUID.randomUUID()).screenId(UUID.randomUUID()).rowLabel("A").seatNumber("1").seatCategory("REGULAR").status("ACTIVE").build()
        ));
    }

    @Test
    void testPublicSeatInventoryRequiresNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/shows/" + showId + "/seat-inventory"))
                .andExpect(status().isOk());
    }

    @Test
    void testHoldsEndpointRequiresAuth() throws Exception {
        // Missing token -> 401 Unauthorized
        mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .content("{\"physicalSeatIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void testGetBookingRequiresAuth() throws Exception {
        UUID bookingId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/bookings/" + bookingId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAnotherUserBookingForbidden() throws Exception {
        UUID anotherUser = UUID.randomUUID();
        Booking booking = Booking.builder()
                .userId(anotherUser)
                .showId(showId)
                .totalAmount(BigDecimal.valueOf(300))
                .currency("INR")
                .status(BookingStatus.CONFIRMED)
                .build();
        booking = bookingRepository.save(booking);

        // Attempt to access with customerToken (customerId != anotherUser)
        mockMvc.perform(get("/api/v1/bookings/" + booking.getBookingId())
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void testInternalEndpointRequiresInternalAuth() throws Exception {
        UUID bookingId = UUID.randomUUID();

        // Without token -> 401 Unauthorized (not permitAll!)
        mockMvc.perform(post("/internal/v1/bookings/" + bookingId + "/apply-payment-pending")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"" + customerId + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void testCustomerReleaseAnotherUserHoldForbidden() throws Exception {
        UUID anotherUser = UUID.randomUUID();
        Booking booking = Booking.builder()
                .userId(anotherUser)
                .showId(showId)
                .totalAmount(BigDecimal.valueOf(150))
                .currency("INR")
                .status(BookingStatus.HELD)
                .build();
        booking = bookingRepository.save(booking);

        SeatHold hold = SeatHold.builder()
                .bookingId(booking.getBookingId())
                .userId(anotherUser)
                .showId(showId)
                .status(HoldStatus.ACTIVE)
                .normalExpiresAt(OffsetDateTime.now().plusMinutes(5))
                .build();
        hold = seatHoldRepository.save(hold);

        mockMvc.perform(delete("/api/v1/holds/" + hold.getHoldId())
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
