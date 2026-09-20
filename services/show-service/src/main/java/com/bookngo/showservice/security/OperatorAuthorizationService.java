package com.bookngo.showservice.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class OperatorAuthorizationService {

    // In-memory assignment registry for operators to theatres (supports dynamic registration/testing)
    private final Map<UUID, Set<UUID>> assignments = new ConcurrentHashMap<>();

    public void registerAssignment(UUID operatorUserId, UUID theatreId) {
        assignments.computeIfAbsent(operatorUserId, k -> ConcurrentHashMap.newKeySet()).add(theatreId);
    }

    public void unregisterAssignment(UUID operatorUserId, UUID theatreId) {
        Set<UUID> theatreIds = assignments.get(operatorUserId);
        if (theatreIds != null) {
            theatreIds.remove(theatreId);
        }
    }

    public void clearAssignments() {
        assignments.clear();
    }

    public void verifyOperatorAssignedToTheatre(UUID theatreId, UUID userId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }

        if (userId == null) {
            throw new AccessDeniedException("User identity is required for theatre operations");
        }

        if (theatreId == null) {
            throw new AccessDeniedException("Theatre ID is required for operator operations");
        }

        // 1. Check in-memory registered assignments
        Set<UUID> registered = assignments.get(userId);
        if (registered != null && registered.contains(theatreId)) {
            return;
        }

        // 2. Check request attributes or headers from JWT claims
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();

            // Check assignedTheatres attribute set by JwtAuthenticationFilter
            @SuppressWarnings("unchecked")
            List<String> tokenAssignedTheatres = (List<String>) request.getAttribute("assignedTheatres");
            if (tokenAssignedTheatres != null && tokenAssignedTheatres.contains(theatreId.toString())) {
                return;
            }

            // Check header if present
            String headerAssigned = request.getHeader("X-Operator-Assigned-Theatres");
            if (headerAssigned != null) {
                String[] theatres = headerAssigned.split(",");
                for (String t : theatres) {
                    if (theatreId.toString().equalsIgnoreCase(t.trim())) {
                        return;
                    }
                }
            }
        }

        throw new AccessDeniedException("Theatre operator is not assigned to this theatre");
    }
}
