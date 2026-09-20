package com.bookngo.movieservice.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookngo.movieservice.dto.MovieCreateRequest;
import com.bookngo.movieservice.dto.MovieDto;
import com.bookngo.movieservice.dto.MoviePageResponse;
import com.bookngo.movieservice.entity.Movie;
import com.bookngo.movieservice.entity.MovieStatus;
import com.bookngo.movieservice.exception.ResourceNotFoundException;
import com.bookngo.movieservice.repository.MovieRepository;
import com.bookngo.movieservice.repository.MovieSpecification;
import com.bookngo.movieservice.service.MovieService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    @Override
    @Transactional(readOnly = true)
    public MoviePageResponse listMovies(String title, String language, String genre, String status, int page, int size) {
        log.debug("Listing movies with filters - title: {}, language: {}, genre: {}, status: {}, page: {}, size: {}",
                title, language, genre, status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<Movie> spec = MovieSpecification.filterBy(title, language, genre, status);
        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        List<MovieDto> dtos = moviePage.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return MoviePageResponse.builder()
                .content(dtos)
                .totalElements(moviePage.getTotalElements())
                .totalPages(moviePage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MovieDto getMovieById(UUID movieId) {
        log.debug("Fetching movie by id: {}", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));
        return toDto(movie);
    }

    @Override
    @Transactional
    public MovieDto createMovie(MovieCreateRequest request) {
        log.info("Creating movie with title: {}", request.getTitle());

        Map<String, Object> metadata = request.getMetadata() != null ? request.getMetadata() : new java.util.HashMap<>();

        Movie movie = Movie.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .language(request.getLanguage().trim())
                .genres(request.getGenres())
                .durationMinutes(request.getDurationMinutes())
                .releaseDate(request.getReleaseDate())
                .posterReference(request.getPosterReference())
                .metadata(metadata)
                .status(MovieStatus.ACTIVE)
                .build();

        Movie saved = movieRepository.save(movie);
        log.info("Movie created successfully with id: {}", saved.getMovieId());
        return toDto(saved);
    }

    @Override
    @Transactional
    public MovieDto updateMovie(UUID movieId, MovieCreateRequest request) {
        log.info("Updating movie with id: {}", movieId);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));

        movie.setTitle(request.getTitle().trim());
        movie.setDescription(request.getDescription());
        movie.setLanguage(request.getLanguage().trim());
        movie.setGenres(request.getGenres());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setPosterReference(request.getPosterReference());
        if (request.getMetadata() != null) {
            movie.setMetadata(request.getMetadata());
        }

        Movie updated = movieRepository.save(movie);
        log.info("Movie updated successfully with id: {}", updated.getMovieId());
        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteMovie(UUID movieId) {
        log.info("Deactivating (soft-deleting) movie with id: {}", movieId);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));

        movie.setStatus(MovieStatus.INACTIVE);
        movieRepository.save(movie);
        log.info("Movie deactivated successfully with id: {}", movieId);
    }

    private MovieDto toDto(Movie movie) {
        return MovieDto.builder()
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .language(movie.getLanguage())
                .genres(movie.getGenres())
                .durationMinutes(movie.getDurationMinutes())
                .releaseDate(movie.getReleaseDate())
                .posterReference(movie.getPosterReference())
                .status(movie.getStatus() != null ? movie.getStatus().name() : null)
                .metadata(movie.getMetadata() != null ? movie.getMetadata() : java.util.Collections.emptyMap())
                .build();
    }
}
