package com.bookngo.paymentservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.paymentservice.entity.Payment;
import com.bookngo.paymentservice.entity.RefundReversal;

@Repository
public interface RefundReversalRepository extends JpaRepository<RefundReversal, UUID> {

    List<RefundReversal> findByPayment(Payment payment);

    List<RefundReversal> findByPaymentOrderByCreatedAtDesc(Payment payment);
}
