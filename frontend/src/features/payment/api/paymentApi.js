import { apiClient } from '../../../lib/apiClient';

/**
 * Initiate payment for a booking in HELD state.
 * Authenticated endpoint: POST /api/v1/payments
 * Header: Idempotency-Key
 * Body: { bookingId: string, providerName: string }
 * Note: Never sends client-controlled amount or currency.
 */
export async function initiatePayment({
  bookingId,
  providerName = 'SIM_SUCCESS',
  idempotencyKey,
  signal,
}) {
  if (!bookingId) throw new Error('Booking ID is required');
  if (!idempotencyKey) {
    throw new Error('Idempotency-Key header is required for payment initiation');
  }

  return apiClient.post(
    '/payments',
    { bookingId, providerName },
    {
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
      signal,
    }
  );
}

/**
 * Poll or get payment status by paymentId.
 * Authenticated endpoint: GET /api/v1/payments/{paymentId}/status
 * States: INITIATED, PENDING, SUCCESS, FAILURE, CANCELLED, TIMEOUT, UNKNOWN
 */
export async function getPaymentStatus(paymentId, { signal } = {}) {
  if (!paymentId) throw new Error('Payment ID is required');
  return apiClient.get(`/payments/${encodeURIComponent(paymentId)}/status`, { signal });
}

/**
 * Trigger payment reconciliation with provider/gateway.
 * Authenticated endpoint: POST /api/v1/payments/{paymentId}/reconcile
 */
export async function reconcilePayment(paymentId, { signal } = {}) {
  if (!paymentId) throw new Error('Payment ID is required');
  return apiClient.post(`/payments/${encodeURIComponent(paymentId)}/reconcile`, {}, { signal });
}

/**
 * Get refund / reversal status for a payment.
 * Authenticated endpoint: GET /api/v1/payments/{paymentId}/refund
 */
export async function getRefundStatus(paymentId, { signal } = {}) {
  if (!paymentId) throw new Error('Payment ID is required');
  return apiClient.get(`/payments/${encodeURIComponent(paymentId)}/refund`, { signal });
}
