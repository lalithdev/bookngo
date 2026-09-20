package com.bookngo.theatreservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.theatreservice.entity.PhysicalSeat;

@Repository
public interface PhysicalSeatRepository extends JpaRepository<PhysicalSeat, UUID> {

    List<PhysicalSeat> findByScreen_ScreenId(UUID screenId);

    boolean existsByScreen_ScreenIdAndRowLabelAndSeatNumber(UUID screenId, String rowLabel, String seatNumber);

    Optional<PhysicalSeat> findByScreen_ScreenIdAndRowLabelAndSeatNumber(UUID screenId, String rowLabel, String seatNumber);
}
