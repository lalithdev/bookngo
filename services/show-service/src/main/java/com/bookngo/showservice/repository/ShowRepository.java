package com.bookngo.showservice.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookngo.showservice.entity.Show;
import com.bookngo.showservice.entity.ShowStatus;

@Repository
public interface ShowRepository extends JpaRepository<Show, UUID>, JpaSpecificationExecutor<Show> {

    List<Show> findByStatus(ShowStatus status);

    @Query("SELECT COUNT(s) > 0 FROM Show s WHERE s.screenId = :screenId " +
           "AND s.status = :status " +
           "AND s.startsAt < :endsAt " +
           "AND s.endsAt > :startsAt")
    boolean existsOverlappingShow(
            @Param("screenId") UUID screenId,
            @Param("status") ShowStatus status,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);
}
