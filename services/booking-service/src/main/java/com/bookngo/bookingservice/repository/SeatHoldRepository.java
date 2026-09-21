package com.bookngo.bookingservice.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookngo.bookingservice.entity.SeatHold;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {
    Optional<SeatHold> findByBookingId(UUID bookingId);

    @Query("SELECT sh FROM SeatHold sh WHERE sh.status = com.bookngo.bookingservice.entity.HoldStatus.ACTIVE AND sh.normalExpiresAt < :now AND (sh.paymentGraceExpiresAt IS NULL OR sh.paymentGraceExpiresAt < :now)")
    List<SeatHold> findExpiredActiveHolds(@Param("now") OffsetDateTime now);
}
