package com.bookngo.paymentservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.paymentservice.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByBookingId(UUID bookingId);

    Optional<Payment> findByProviderNameAndProviderPaymentReference(String providerName, String providerPaymentReference);

    List<Payment> findByUserId(UUID userId);
}
