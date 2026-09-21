package com.bookngo.bookingservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.bookngo.bookingservice.entity.TicketStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private UUID ticketId;
    private UUID bookingId;
    private String ticketCode;
    private TicketStatus status;
    private Object historicalSnapshot;
    private OffsetDateTime issuedAt;
}
