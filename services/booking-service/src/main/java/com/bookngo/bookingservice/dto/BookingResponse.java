package com.bookngo.bookingservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.bookngo.bookingservice.entity.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private UUID bookingId;
    private UUID userId;
    private UUID showId;
    private BigDecimal totalAmount;
    private String currency;
    private BookingStatus status;
    private OffsetDateTime paymentGraceExpiresAt;
    private UUID paymentId;
    private List<BookingSeatResponse> bookingSeats;
    private OffsetDateTime createdAt;
}
