package com.bookngo.paymentservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.paymentservice.entity.Payment;
import com.bookngo.paymentservice.entity.PaymentReconciliation;

@Repository
public interface PaymentReconciliationRepository extends JpaRepository<PaymentReconciliation, UUID> {

    List<PaymentReconciliation> findByPaymentOrderByCheckedAtDesc(Payment payment);
}
