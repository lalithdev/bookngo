package com.bookngo.paymentservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.paymentservice.entity.PaymentIdempotencyRecord;

@Repository
public interface PaymentIdempotencyRecordRepository extends JpaRepository<PaymentIdempotencyRecord, UUID> {

    Optional<PaymentIdempotencyRecord> findByUserIdAndOperationTypeAndIdempotencyKey(
            UUID userId, String operationType, String idempotencyKey);
}
