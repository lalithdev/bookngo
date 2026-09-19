package com.bookngo.userservice.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookngo.userservice.entity.OtpVerification;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification> findFirstByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.phoneNumber = :phoneNumber AND o.createdAt >= :since")
    long countByPhoneNumberAndCreatedAtAfter(@Param("phoneNumber") String phoneNumber, @Param("since") OffsetDateTime since);
}
