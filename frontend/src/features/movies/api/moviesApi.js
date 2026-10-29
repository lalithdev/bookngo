import { apiClient } from '../../../lib/apiClient';

/**
 * Fetch movies catalog from Movie Service via API Gateway.
 * Supported filters per OpenAPI contract: status, title, language, genre, page, size.
 * Note: Movie Service does NOT filter by city.
 */
export async function fetchMovies({ status = 'ACTIVE', title, language, genre, signal } = {}) {
  const queryParams = new URLSearchParams();

  if (status) queryParams.set('status', status);
  if (title && title.trim()) queryParams.set('title', title.trim());
  if (language && language.trim()) queryParams.set('language', language.trim());
  if (genre && genre.trim()) queryParams.set('genre', genre.trim());

  const queryString = queryParams.toString();
  const path = `/movies${queryString ? `?${queryString}` : ''}`;

  const response = await apiClient.get(path, { signal });
  // Ensure array return even if paginated envelope or raw array
  if (Array.isArray(response)) {
    return response;
  }
  if (response && Array.isArray(response.content)) {
    return response.content;
  }
  return [];
}

/**
 * Fetch detailed movie entity by ID from Movie Service.
 */
export async function fetchMovieById(movieId, { signal } = {}) {
  if (!movieId) {
    throw new Error('Movie ID is required');
  }
  return apiClient.get(`/movies/${encodeURIComponent(movieId)}`, { signal });
}

/**
 * ADMIN: Create new movie.
 * POST /api/v1/movies
 */
export async function createMovie(movieData, { signal } = {}) {
  return apiClient.post('/movies', movieData, { signal });
}

/**
 * ADMIN: Update existing movie.
 * PUT /api/v1/movies/{movieId}
 */
export async function updateMovie(movieId, movieData, { signal } = {}) {
  if (!movieId) throw new Error('Movie ID is required');
  return apiClient.put(`/movies/${encodeURIComponent(movieId)}`, movieData, { signal });
}

/**
 * ADMIN: Delete movie.
 * DELETE /api/v1/movies/{movieId}
 */
export async function deleteMovie(movieId, { signal } = {}) {
  if (!movieId) throw new Error('Movie ID is required');
  return apiClient.delete(`/movies/${encodeURIComponent(movieId)}`, { signal });
}

// Convenient Aliases
export const getMovies = fetchMovies;
export const getMovieById = fetchMovieById;
