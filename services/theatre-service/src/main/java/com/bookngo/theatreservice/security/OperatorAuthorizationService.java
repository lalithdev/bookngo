package com.bookngo.theatreservice.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.bookngo.theatreservice.entity.OperatorAssignmentStatus;
import com.bookngo.theatreservice.repository.TheatreOperatorAssignmentRepository;

@Service
public class OperatorAuthorizationService {

    private final TheatreOperatorAssignmentRepository assignmentRepository;

    public OperatorAuthorizationService(TheatreOperatorAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    public void verifyOperatorAssignedToTheatre(UUID theatreId, UUID userId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }

        if (userId == null) {
            throw new AccessDeniedException("User identity is required for theatre operations");
        }

        boolean assigned = assignmentRepository.existsByTheatre_TheatreIdAndUserIdAndStatus(
                theatreId, userId, OperatorAssignmentStatus.ACTIVE);

        if (!assigned) {
            throw new AccessDeniedException("Theatre operator is not assigned to this theatre");
        }
    }
}
