package com.bookngo.paymentservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.paymentservice.entity.Payment;
import com.bookngo.paymentservice.entity.PaymentAttempt;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findByPayment(Payment payment);

    Optional<PaymentAttempt> findFirstByPaymentOrderByInitiatedAtDesc(Payment payment);
}
