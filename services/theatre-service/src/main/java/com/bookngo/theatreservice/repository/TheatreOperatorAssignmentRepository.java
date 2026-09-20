package com.bookngo.theatreservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.theatreservice.entity.OperatorAssignmentStatus;
import com.bookngo.theatreservice.entity.TheatreOperatorAssignment;

@Repository
public interface TheatreOperatorAssignmentRepository extends JpaRepository<TheatreOperatorAssignment, UUID> {

    boolean existsByTheatre_TheatreIdAndUserIdAndStatus(UUID theatreId, UUID userId, OperatorAssignmentStatus status);

    Optional<TheatreOperatorAssignment> findByTheatre_TheatreIdAndUserId(UUID theatreId, UUID userId);

    List<TheatreOperatorAssignment> findByTheatre_TheatreId(UUID theatreId);

    List<TheatreOperatorAssignment> findByUserIdAndStatus(UUID userId, OperatorAssignmentStatus status);
}
