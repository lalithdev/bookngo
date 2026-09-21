package com.bookngo.bookingservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.bookingservice.entity.BookingIdempotencyRecord;

@Repository
public interface BookingIdempotencyRecordRepository extends JpaRepository<BookingIdempotencyRecord, UUID> {
    Optional<BookingIdempotencyRecord> findByUserIdAndOperationTypeAndIdempotencyKey(
            UUID userId, String operationType, String idempotencyKey);
}
