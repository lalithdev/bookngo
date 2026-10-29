import { apiClient } from '../../../lib/apiClient';

/**
 * Fetch verified theatres for a city or all theatres.
 * Public endpoint: GET /api/v1/theatres?city={city}
 */
export async function fetchTheatresByCity(city = 'Vijayawada', { signal } = {}) {
  const queryParams = new URLSearchParams();
  if (city && city.trim()) {
    queryParams.set('city', city.trim());
  }
  const queryString = queryParams.toString();
  const path = `/theatres${queryString ? `?${queryString}` : ''}`;
  const response = await apiClient.get(path, { signal });
  return Array.isArray(response) ? response : [];
}

/**
 * Fetch theatre by ID.
 * Public endpoint: GET /api/v1/theatres/{theatreId}
 */
export async function fetchTheatreById(theatreId, { signal } = {}) {
  if (!theatreId) throw new Error('Theatre ID is required');
  return apiClient.get(`/theatres/${encodeURIComponent(theatreId)}`, { signal });
}

/**
 * Fetch screens for a theatre.
 * Public endpoint: GET /api/v1/theatres/{theatreId}/screens
 */
export async function fetchScreensByTheatre(theatreId, { signal } = {}) {
  if (!theatreId) throw new Error('Theatre ID is required');
  const response = await apiClient.get(`/theatres/${encodeURIComponent(theatreId)}/screens`, { signal });
  return Array.isArray(response) ? response : [];
}

/**
 * Fetch physical seat configuration for a screen.
 * Public endpoint: GET /api/v1/screens/{screenId}/seats
 */
export async function fetchPhysicalSeatsByScreen(screenId, { signal } = {}) {
  if (!screenId) throw new Error('Screen ID is required');
  const response = await apiClient.get(`/screens/${encodeURIComponent(screenId)}/seats`, { signal });
  return Array.isArray(response) ? response : [];
}

/**
 * Search scheduled movie shows with flexible filtering.
 * Public endpoint: GET /api/v1/shows
 * Query params: movieId, theatreId, city, date (YYYY-MM-DD), language, format
 */
export async function fetchShows({
  movieId,
  theatreId,
  city = 'Vijayawada',
  date,
  language,
  format,
  signal,
} = {}) {
  const queryParams = new URLSearchParams();
  if (movieId) queryParams.set('movieId', movieId);
  if (theatreId) queryParams.set('theatreId', theatreId);
  if (city && city.trim()) queryParams.set('city', city.trim());
  if (date) queryParams.set('date', date);
  if (language && language.trim()) queryParams.set('language', language.trim());
  if (format && format.trim()) queryParams.set('format', format.trim());

  const queryString = queryParams.toString();
  const path = `/shows${queryString ? `?${queryString}` : ''}`;
  const response = await apiClient.get(path, { signal });
  return Array.isArray(response) ? response : [];
}

/**
 * Fetch detailed show entity by showId.
 * Public endpoint: GET /api/v1/shows/{showId}
 */
export async function fetchShowById(showId, { signal } = {}) {
  if (!showId) throw new Error('Show ID is required');
  return apiClient.get(`/shows/${encodeURIComponent(showId)}`, { signal });
}

/**
 * Fetch show pricing per seat category.
 * Public endpoint: GET /api/v1/shows/{showId}/pricing
 */
export async function fetchShowPricing(showId, { signal } = {}) {
  if (!showId) throw new Error('Show ID is required');
  const response = await apiClient.get(`/shows/${encodeURIComponent(showId)}/pricing`, { signal });
  return Array.isArray(response) ? response : [];
}

/**
 * Fetch cancellation policy for a show.
 * Public endpoint: GET /api/v1/shows/{showId}/cancellation-policy
 */
export async function fetchCancellationPolicy(showId, { signal } = {}) {
  if (!showId) throw new Error('Show ID is required');
  return apiClient.get(`/shows/${encodeURIComponent(showId)}/cancellation-policy`, { signal });
}

/**
 * OPERATOR / ADMIN ENDPOINTS
 */

/**
 * Create a new theatre.
 * POST /api/v1/theatres
 */
export async function createTheatre(theatreData, { signal } = {}) {
  return apiClient.post('/theatres', theatreData, { signal });
}

/**
 * Update an existing theatre.
 * PUT /api/v1/theatres/{theatreId}
 */
export async function updateTheatre(theatreId, theatreData, { signal } = {}) {
  if (!theatreId) throw new Error('Theatre ID is required');
  return apiClient.put(`/theatres/${encodeURIComponent(theatreId)}`, theatreData, { signal });
}

/**
 * Create a screen in a theatre.
 * POST /api/v1/theatres/{theatreId}/screens
 */
export async function createScreen(theatreId, screenData, { signal } = {}) {
  if (!theatreId) throw new Error('Theatre ID is required');
  return apiClient.post(`/theatres/${encodeURIComponent(theatreId)}/screens`, screenData, { signal });
}

/**
 * Configure physical seats for a screen.
 * POST /api/v1/screens/{screenId}/seats
 */
export async function configureScreenSeats(screenId, seatData, { signal } = {}) {
  if (!screenId) throw new Error('Screen ID is required');
  return apiClient.post(`/screens/${encodeURIComponent(screenId)}/seats`, seatData, { signal });
}

/**
 * Schedule a new movie show.
 * POST /api/v1/shows
 */
export async function createShow(showData, { signal } = {}) {
  return apiClient.post('/shows', showData, { signal });
}

/**
 * Update pricing for a show.
 * PUT /api/v1/shows/{showId}/pricing
 */
export async function updateShowPricing(showId, pricingData, { signal } = {}) {
  if (!showId) throw new Error('Show ID is required');
  return apiClient.put(`/shows/${encodeURIComponent(showId)}/pricing`, pricingData, { signal });
}

/**
 * Update cancellation policy for a show.
 * PUT /api/v1/shows/{showId}/cancellation-policy
 */
export async function updateCancellationPolicy(showId, policyData, { signal } = {}) {
  if (!showId) throw new Error('Show ID is required');
  return apiClient.put(`/shows/${encodeURIComponent(showId)}/cancellation-policy`, policyData, { signal });
}

// Convenient Aliases
export const fetchShowsByCity = (city = 'Vijayawada', opts = {}) => fetchShows({ city, ...opts });
export const getTheatres = fetchTheatresByCity;
export const getTheatreById = fetchTheatreById;
export const getScreensByTheatre = fetchScreensByTheatre;
export const getPhysicalSeatsByScreen = fetchPhysicalSeatsByScreen;
export const getShows = fetchShows;
export const getShowById = fetchShowById;
export const getShowPricing = fetchShowPricing;
export const getCancellationPolicyByShow = fetchCancellationPolicy;
