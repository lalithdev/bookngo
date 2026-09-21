package com.bookngo.bookingservice.dto;

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
public class PaymentOutcomeInternalResponse {
    private UUID bookingId;
    private BookingStatus bookingStatus;
    private boolean ticketIssued;
}
