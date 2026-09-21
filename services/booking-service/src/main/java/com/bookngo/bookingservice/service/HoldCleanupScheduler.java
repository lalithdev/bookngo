package com.bookngo.bookingservice.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bookngo.bookingservice.entity.Booking;
import com.bookngo.bookingservice.entity.BookingStatus;
import com.bookngo.bookingservice.entity.HoldStatus;
import com.bookngo.bookingservice.entity.InventoryStatus;
import com.bookngo.bookingservice.entity.SeatHold;
import com.bookngo.bookingservice.entity.ShowSeatInventory;
import com.bookngo.bookingservice.repository.BookingRepository;
import com.bookngo.bookingservice.repository.SeatHoldRepository;
import com.bookngo.bookingservice.repository.ShowSeatInventoryRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class HoldCleanupScheduler {

    private final SeatHoldRepository seatHoldRepository;
    private final BookingRepository bookingRepository;
    private final ShowSeatInventoryRepository showSeatInventoryRepository;

    public HoldCleanupScheduler(
            SeatHoldRepository seatHoldRepository,
            BookingRepository bookingRepository,
            ShowSeatInventoryRepository showSeatInventoryRepository) {
        this.seatHoldRepository = seatHoldRepository;
        this.bookingRepository = bookingRepository;
        this.showSeatInventoryRepository = showSeatInventoryRepository;
    }

    /**
     * Supplementary background cleanup sweeper running every 30 seconds.
     * Note: Core correctness is maintained lazily during read/hold transactions.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    @Transactional
    public void sweepExpiredHolds() {
        OffsetDateTime now = OffsetDateTime.now();
        List<SeatHold> expiredHolds = seatHoldRepository.findExpiredActiveHolds(now);

        if (expiredHolds.isEmpty()) {
            return;
        }

        log.debug("Sweeper found {} expired active holds to clean up", expiredHolds.size());

        for (SeatHold hold : expiredHolds) {
            try {
                hold.setStatus(HoldStatus.EXPIRED);
                seatHoldRepository.save(hold);

                bookingRepository.findById(hold.getBookingId()).ifPresent(booking -> {
                    if (booking.getStatus() == BookingStatus.HELD || booking.getStatus() == BookingStatus.INITIATED) {
                        booking.setStatus(BookingStatus.EXPIRED);
                        bookingRepository.save(booking);
                    }
                });

                List<ShowSeatInventory> inventory = showSeatInventoryRepository.findByActiveHoldId(hold.getHoldId());
                for (ShowSeatInventory item : inventory) {
                    item.setStatus(InventoryStatus.AVAILABLE);
                    item.setActiveHoldId(null);
                    item.setHoldExpiresAt(null);
                    showSeatInventoryRepository.save(item);
                }
            } catch (Exception e) {
                log.warn("Error sweeping expired hold {}: {}", hold.getHoldId(), e.getMessage());
            }
        }
    }
}
