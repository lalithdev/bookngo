package com.bookngo.movieservice.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.bookngo.movieservice.entity.Movie;
import com.bookngo.movieservice.entity.MovieStatus;

import jakarta.persistence.criteria.Predicate;

public class MovieSpecification {

    private MovieSpecification() {
    }

    public static Specification<Movie> filterBy(String title, String language, String genre, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%"));
            }

            if (language != null && !language.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("language")), language.trim().toLowerCase()));
            }

            if (genre != null && !genre.trim().isEmpty()) {
                // In genres JSON string array like ["Action", "Sci-Fi"], check case-insensitive match
                predicates.add(cb.like(cb.lower(root.get("genres")), "%\"" + genre.trim().toLowerCase() + "\"%"));
            }

            // Status filter: defaults to ACTIVE if not supplied or blank
            if (status != null && !status.trim().isEmpty()) {
                try {
                    MovieStatus movieStatus = MovieStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), movieStatus));
                } catch (IllegalArgumentException e) {
                    // If invalid status is passed, match nothing
                    predicates.add(cb.disjunction());
                }
            } else {
                predicates.add(cb.equal(root.get("status"), MovieStatus.ACTIVE));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
