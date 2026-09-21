package com.bookngo.bookingservice.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookngo.bookingservice.entity.ShowSeatInventory;

import jakarta.persistence.LockModeType;

@Repository
public interface ShowSeatInventoryRepository extends JpaRepository<ShowSeatInventory, UUID> {

    List<ShowSeatInventory> findByShowId(UUID showId);

    long countByShowId(UUID showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ssi FROM ShowSeatInventory ssi WHERE ssi.showId = :showId AND ssi.physicalSeatId IN :physicalSeatIds ORDER BY ssi.physicalSeatId ASC")
    List<ShowSeatInventory> findByShowIdAndPhysicalSeatIdInOrderAscForUpdate(
            @Param("showId") UUID showId,
            @Param("physicalSeatIds") Collection<UUID> physicalSeatIds);

    List<ShowSeatInventory> findByActiveHoldId(UUID activeHoldId);

    List<ShowSeatInventory> findByCurrentBookingId(UUID currentBookingId);

    @Modifying
    @Query(value = "INSERT INTO show_seat_inventory (show_seat_inventory_id, show_id, physical_seat_id, status, created_at, updated_at) " +
                   "VALUES (:id, :showId, :physicalSeatId, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                   "ON CONFLICT (show_id, physical_seat_id) DO NOTHING", nativeQuery = true)
    int insertInventoryIfAbsent(
            @Param("id") UUID id,
            @Param("showId") UUID showId,
            @Param("physicalSeatId") UUID physicalSeatId);
}
