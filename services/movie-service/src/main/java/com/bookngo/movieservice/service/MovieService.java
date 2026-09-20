package com.bookngo.movieservice.service;

import java.util.UUID;

import com.bookngo.movieservice.dto.MovieCreateRequest;
import com.bookngo.movieservice.dto.MovieDto;
import com.bookngo.movieservice.dto.MoviePageResponse;

public interface MovieService {

    MoviePageResponse listMovies(String title, String language, String genre, String status, int page, int size);

    MovieDto getMovieById(UUID movieId);

    MovieDto createMovie(MovieCreateRequest request);

    MovieDto updateMovie(UUID movieId, MovieCreateRequest request);

    void deleteMovie(UUID movieId);
}
