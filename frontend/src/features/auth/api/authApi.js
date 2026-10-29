import { apiClient } from '../../../lib/apiClient';

/**
 * Customer Phone OTP Authentication & Password Login API.
 * Adheres strictly to the User Service contract.
 */

/**
 * Request an OTP for the supplied phone number.
 * Endpoint: POST /api/v1/auth/otp/request
 * Payload: { phoneNumber: string }
 */
export async function requestOtp(phoneNumber, { signal } = {}) {
  return apiClient.post('/auth/otp/request', { phoneNumber }, { signal });
}

/**
 * Verify OTP code and obtain signed JWT token.
 * Endpoint: POST /api/v1/auth/otp/verify
 * Payload: { phoneNumber: string, otpCode: string }
 * Response: { token: string, tokenType: string, expiresIn: number, user: object }
 */
export async function verifyOtp(phoneNumber, otpCode, { signal } = {}) {
  return apiClient.post('/auth/otp/verify', { phoneNumber, otpCode }, { signal });
}

/**
 * Operator / Admin password login.
 * Endpoint: POST /api/v1/auth/login
 * Payload: { username: string, password: string }
 */
export async function loginPassword(username, password, { signal } = {}) {
  return apiClient.post('/auth/login', { username, password }, { signal });
}

/**
 * Retrieve authenticated customer profile.
 * Endpoint: GET /api/v1/users/me
 */
export async function getCurrentUserProfile({ signal } = {}) {
  return apiClient.get('/users/me', { signal });
}

/**
 * Update authenticated customer profile.
 * Endpoint: PUT /api/v1/users/me
 * Payload: { fullName?: string, email?: string }
 */
export async function updateCurrentUserProfile(profileData, { signal } = {}) {
  return apiClient.put('/users/me', profileData, { signal });
}
