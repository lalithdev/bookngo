package com.bookngo.movieservice.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.bookngo.movieservice.dto.MovieCreateRequest;
import com.bookngo.movieservice.dto.MovieDto;
import com.bookngo.movieservice.dto.MoviePageResponse;
import com.bookngo.movieservice.entity.Movie;
import com.bookngo.movieservice.entity.MovieStatus;
import com.bookngo.movieservice.exception.ResourceNotFoundException;
import com.bookngo.movieservice.repository.MovieRepository;
import com.bookngo.movieservice.service.impl.MovieServiceImpl;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    private MovieServiceImpl movieService;

    private UUID movieId;
    private Movie movie;

    @BeforeEach
    void setUp() {
        movieService = new MovieServiceImpl(movieRepository);
        movieId = UUID.randomUUID();
        movie = Movie.builder()
                .movieId(movieId)
                .title("Inception")
                .description("A thief who steals corporate secrets through the use of dream-sharing technology.")
                .language("English")
                .genres(List.of("Action", "Sci-Fi"))
                .durationMinutes(148)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .posterReference("https://example.com/inception.jpg")
                .metadata(java.util.Map.of("director", "Christopher Nolan", "tags", List.of("dreams", "heist")))
                .status(MovieStatus.ACTIVE)
                .build();
    }

    @Test
    void testCreateMovie_Success() {
        java.util.Map<String, Object> metadata = java.util.Map.of(
                "director", "Christopher Nolan",
                "cast", List.of("Matthew McConaughey", "Anne Hathaway"),
                "boxOffice", java.util.Map.of("budget", 165000000)
        );

        MovieCreateRequest request = MovieCreateRequest.builder()
                .title("Interstellar")
                .description("A team of explorers travel through a wormhole in space.")
                .language("English")
                .genres(List.of("Adventure", "Drama", "Sci-Fi"))
                .durationMinutes(169)
                .releaseDate(LocalDate.of(2014, 11, 7))
                .posterReference("https://example.com/interstellar.jpg")
                .metadata(metadata)
                .build();

        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> {
            Movie m = invocation.getArgument(0);
            m.setMovieId(UUID.randomUUID());
            return m;
        });

        MovieDto created = movieService.createMovie(request);

        assertNotNull(created);
        assertNotNull(created.getMovieId());
        assertEquals("Interstellar", created.getTitle());
        assertEquals("ACTIVE", created.getStatus());
        assertNotNull(created.getMetadata());
        assertEquals("Christopher Nolan", created.getMetadata().get("director"));
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void testGetMovieById_Success() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));

        MovieDto found = movieService.getMovieById(movieId);

        assertNotNull(found);
        assertEquals(movieId, found.getMovieId());
        assertEquals("Inception", found.getTitle());
        assertNotNull(found.getMetadata());
        assertEquals("Christopher Nolan", found.getMetadata().get("director"));
    }

    @Test
    void testGetMovieById_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(movieRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> movieService.getMovieById(nonExistentId));
    }

    @Test
    void testUpdateMovie_Success() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        java.util.Map<String, Object> updatedMetadata = java.util.Map.of("director", "Christopher Nolan", "remasteredYear", 2026);

        MovieCreateRequest updateRequest = MovieCreateRequest.builder()
                .title("Inception - Remastered")
                .description("Updated description")
                .language("English")
                .genres(List.of("Action", "Sci-Fi", "Thriller"))
                .durationMinutes(150)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .posterReference("https://example.com/inception_remastered.jpg")
                .metadata(updatedMetadata)
                .build();

        MovieDto updated = movieService.updateMovie(movieId, updateRequest);

        assertNotNull(updated);
        assertEquals("Inception - Remastered", updated.getTitle());
        assertEquals(150, updated.getDurationMinutes());
        assertEquals(2026, updated.getMetadata().get("remasteredYear"));
        verify(movieRepository).save(movie);
    }

    @Test
    void testUpdateMovie_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(movieRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        MovieCreateRequest updateRequest = MovieCreateRequest.builder()
                .title("Title")
                .language("English")
                .genres(List.of("Action"))
                .durationMinutes(120)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> movieService.updateMovie(nonExistentId, updateRequest));
    }

    @Test
    void testDeleteMovie_SoftDeletes_SetsInactive() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        movieService.deleteMovie(movieId);

        assertEquals(MovieStatus.INACTIVE, movie.getStatus());
        verify(movieRepository).save(movie);
    }

    @Test
    void testDeleteMovie_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(movieRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> movieService.deleteMovie(nonExistentId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testListMovies_ReturnsPageResponse() {
        Page<Movie> page = new PageImpl<>(List.of(movie));
        when(movieRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        MoviePageResponse response = movieService.listMovies("Inception", "English", "Action", "ACTIVE", 0, 20);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals("Inception", response.getContent().get(0).getTitle());
    }
}
