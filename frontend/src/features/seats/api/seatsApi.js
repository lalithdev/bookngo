import { apiClient } from '../../../lib/apiClient';

/**
 * Fetch real-time show seat availability.
 * Public endpoint: GET /api/v1/shows/{showId}/seat-inventory
 * Returns: Array of SeatInventoryItem { showSeatInventoryId, physicalSeatId, status, holdExpiresAt }
 */
export async function fetchShowSeatInventory(showId, { signal } = {}) {
  if (!showId) throw new Error('Show ID is required');
  const response = await apiClient.get(`/shows/${encodeURIComponent(showId)}/seat-inventory`, { signal });
  return Array.isArray(response) ? response : [];
}

/**
 * Create atomic seat hold (1-6 seats) and initialize booking.
 * Authenticated endpoint: POST /api/v1/shows/{showId}/holds
 * Header: Idempotency-Key
 * Body: { physicalSeatIds: string[] }
 * Returns: SeatHoldResponse { holdId, bookingId, showId, status, normalExpiresAt, paymentGraceExpiresAt, totalAmount, currency }
 */
export async function createSeatHold(showId, physicalSeatIds, idempotencyKey, { signal } = {}) {
  if (!showId) throw new Error('Show ID is required');
  if (!Array.isArray(physicalSeatIds) || physicalSeatIds.length === 0) {
    throw new Error('At least one physical seat ID is required');
  }
  if (!idempotencyKey) {
    throw new Error('Idempotency-Key header is required for seat hold creation');
  }

  return apiClient.post(
    `/shows/${encodeURIComponent(showId)}/holds`,
    { physicalSeatIds },
    {
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
      signal,
    }
  );
}

/**
 * Retrieve status and remaining time of an active hold.
 * Authenticated endpoint: GET /api/v1/holds/{holdId}
 */
export async function getHoldById(holdId, { signal } = {}) {
  if (!holdId) throw new Error('Hold ID is required');
  return apiClient.get(`/holds/${encodeURIComponent(holdId)}`, { signal });
}

/**
 * Release an unpaid active hold before expiry.
 * Authenticated endpoint: DELETE /api/v1/holds/{holdId}
 */
export async function releaseHold(holdId, { signal } = {}) {
  if (!holdId) throw new Error('Hold ID is required');
  return apiClient.delete(`/holds/${encodeURIComponent(holdId)}`, { signal });
}

// Convenient Alias
export const fetchSeatInventory = fetchShowSeatInventory;
