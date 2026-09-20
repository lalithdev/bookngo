package com.bookngo.showservice.repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.bookngo.showservice.entity.Show;
import com.bookngo.showservice.entity.ShowStatus;

import jakarta.persistence.criteria.Predicate;

public class ShowSpecification {

    public static Specification<Show> filterShows(
            ShowStatus status,
            UUID movieId,
            UUID theatreId,
            List<UUID> theatreIds,
            List<UUID> movieIds,
            OffsetDateTime startFrom,
            OffsetDateTime startTo,
            String format) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (movieId != null) {
                predicates.add(cb.equal(root.get("movieId"), movieId));
            }

            if (theatreId != null) {
                predicates.add(cb.equal(root.get("theatreId"), theatreId));
            }

            if (theatreIds != null && !theatreIds.isEmpty()) {
                predicates.add(root.get("theatreId").in(theatreIds));
            }

            if (movieIds != null && !movieIds.isEmpty()) {
                predicates.add(root.get("movieId").in(movieIds));
            }

            if (startFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startsAt"), startFrom));
            }

            if (startTo != null) {
                predicates.add(cb.lessThan(root.get("startsAt"), startTo));
            }

            if (format != null && !format.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("showFormat")), format.trim().toLowerCase()));
            }

            query.orderBy(cb.asc(root.get("startsAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
