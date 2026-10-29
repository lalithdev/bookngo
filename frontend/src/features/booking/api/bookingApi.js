import { apiClient } from '../../../lib/apiClient';

/**
 * List booking history for current logged-in customer.
 * Authenticated endpoint: GET /api/v1/bookings/me
 */
export async function getMyBookings({ signal } = {}) {
  const response = await apiClient.get('/bookings/me', { signal });
  return Array.isArray(response) ? response : [];
}

/**
 * Get booking details by ID.
 * Authenticated endpoint: GET /api/v1/bookings/{bookingId}
 */
export async function getBookingById(bookingId, { signal } = {}) {
  if (!bookingId) throw new Error('Booking ID is required');
  return apiClient.get(`/bookings/${encodeURIComponent(bookingId)}`, { signal });
}

/**
 * Cancel a confirmed booking if eligible by policy.
 * Authenticated endpoint: POST /api/v1/bookings/{bookingId}/cancel
 */
export async function cancelBooking(bookingId, { signal } = {}) {
  if (!bookingId) throw new Error('Booking ID is required');
  return apiClient.post(`/bookings/${encodeURIComponent(bookingId)}/cancel`, {}, { signal });
}

/**
 * Get digital ticket for confirmed booking.
 * Authenticated endpoint: GET /api/v1/bookings/{bookingId}/ticket
 */
export async function getTicketByBooking(bookingId, { signal } = {}) {
  if (!bookingId) throw new Error('Booking ID is required');
  return apiClient.get(`/bookings/${encodeURIComponent(bookingId)}/ticket`, { signal });
}
