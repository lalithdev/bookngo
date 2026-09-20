package com.bookngo.showservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.showservice.entity.CancellationPolicy;
import com.bookngo.showservice.entity.PolicyStatus;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, UUID> {

    Optional<CancellationPolicy> findByShow_ShowId(UUID showId);

    Optional<CancellationPolicy> findByShow_ShowIdAndStatus(UUID showId, PolicyStatus status);

    boolean existsByShow_ShowId(UUID showId);
}
