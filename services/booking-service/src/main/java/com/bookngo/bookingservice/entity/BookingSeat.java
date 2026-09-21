package com.bookngo.bookingservice.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "booking_seats",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_booking_seats_booking_physical_seat",
            columnNames = {"booking_id", "physical_seat_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_seat_id", updatable = false, nullable = false)
    private UUID bookingSeatId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "show_seat_inventory_id", nullable = false)
    private UUID showSeatInventoryId;

    @Column(name = "physical_seat_id", nullable = false)
    private UUID physicalSeatId;

    @Column(name = "seat_label_snapshot", nullable = false, length = 64)
    private String seatLabelSnapshot;

    @Column(name = "seat_category_snapshot", nullable = false, length = 32)
    private String seatCategorySnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
