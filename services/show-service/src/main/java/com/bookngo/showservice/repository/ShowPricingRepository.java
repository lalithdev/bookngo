package com.bookngo.showservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.showservice.entity.PricingStatus;
import com.bookngo.showservice.entity.ShowPricing;

@Repository
public interface ShowPricingRepository extends JpaRepository<ShowPricing, UUID> {

    List<ShowPricing> findByShow_ShowIdAndStatus(UUID showId, PricingStatus status);

    List<ShowPricing> findByShow_ShowId(UUID showId);

    Optional<ShowPricing> findByShow_ShowIdAndSeatCategoryAndStatus(UUID showId, String seatCategory, PricingStatus status);

    Optional<ShowPricing> findByShow_ShowIdAndSeatCategory(UUID showId, String seatCategory);
}
