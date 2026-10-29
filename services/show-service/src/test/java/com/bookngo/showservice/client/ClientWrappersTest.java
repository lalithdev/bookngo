package com.bookngo.showservice.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bookngo.showservice.service.MovieServiceClient;
import com.bookngo.showservice.service.TheatreServiceClient;

@ExtendWith(MockitoExtension.class)
class ClientWrappersTest {

    @Mock
    private MovieFeignClient movieFeignClient;

    @Mock
    private TheatreFeignClient theatreFeignClient;

    private MovieServiceClient movieServiceClient;
    private TheatreServiceClient theatreServiceClient;

    private final UUID movieId = UUID.randomUUID();
    private final UUID theatreId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        movieServiceClient = new MovieServiceClient(movieFeignClient);
        theatreServiceClient = new TheatreServiceClient(theatreFeignClient);
    }

    @Test
    @DisplayName("MovieServiceClient.getMovieIdsByLanguage queries MovieFeignClient and maps UUIDs")
    void getMovieIdsByLanguage_success() {
        when(movieFeignClient.getMovies("English"))
                .thenReturn(List.of(Map.of("movieId", movieId.toString(), "title", "Inception")));

        List<UUID> ids = movieServiceClient.getMovieIdsByLanguage("English");

        assertEquals(1, ids.size());
        assertEquals(movieId, ids.get(0));
        verify(movieFeignClient).getMovies("English");
    }

    @Test
    @DisplayName("MovieServiceClient.getMovieIdsByLanguage returns empty list on downstream error")
    void getMovieIdsByLanguage_handlesFailure() {
        when(movieFeignClient.getMovies("English")).thenThrow(new RuntimeException("Service unavailable"));

        List<UUID> ids = movieServiceClient.getMovieIdsByLanguage("English");

        assertTrue(ids.isEmpty());
    }

    @Test
    @DisplayName("MovieServiceClient.getMovieIdsByLanguage returns empty list for null/empty input")
    void getMovieIdsByLanguage_emptyInput() {
        List<UUID> idsNull = movieServiceClient.getMovieIdsByLanguage(null);
        List<UUID> idsEmpty = movieServiceClient.getMovieIdsByLanguage("   ");

        assertTrue(idsNull.isEmpty());
        assertTrue(idsEmpty.isEmpty());
    }

    @Test
    @DisplayName("TheatreServiceClient.getTheatreIdsByCity queries TheatreFeignClient and maps UUIDs")
    void getTheatreIdsByCity_success() {
        when(theatreFeignClient.getTheatres("Mumbai"))
                .thenReturn(List.of(Map.of("theatreId", theatreId.toString(), "name", "PVR Cinemas")));

        List<UUID> ids = theatreServiceClient.getTheatreIdsByCity("Mumbai");

        assertEquals(1, ids.size());
        assertEquals(theatreId, ids.get(0));
        verify(theatreFeignClient).getTheatres("Mumbai");
    }

    @Test
    @DisplayName("TheatreServiceClient.getTheatreIdsByCity returns empty list on downstream error")
    void getTheatreIdsByCity_handlesFailure() {
        when(theatreFeignClient.getTheatres("Mumbai")).thenThrow(new RuntimeException("Service unavailable"));

        List<UUID> ids = theatreServiceClient.getTheatreIdsByCity("Mumbai");

        assertTrue(ids.isEmpty());
    }

    @Test
    @DisplayName("TheatreServiceClient.getTheatreIdsByCity returns empty list for null/empty input")
    void getTheatreIdsByCity_emptyInput() {
        List<UUID> idsNull = theatreServiceClient.getTheatreIdsByCity(null);
        List<UUID> idsEmpty = theatreServiceClient.getTheatreIdsByCity("   ");

        assertTrue(idsNull.isEmpty());
        assertTrue(idsEmpty.isEmpty());
    }
}

