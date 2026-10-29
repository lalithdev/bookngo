/**
 * Centralized API Client for BookNGo.
 * Communicates exclusively with the API Gateway (/api/v1).
 * Features:
 * - Base URL resolution from VITE_API_BASE_URL (defaults to /api/v1 via Vite proxy)
 * - Automatic JWT bearer token injection from storage
 * - Structured ApiError normalization
 * - Native AbortController signal support
 * - Event-based 401 Unauthorized notification without circular AuthContext dependency
 */

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/+$/, '');

export class ApiError extends Error {
  constructor(status, message, code = 'API_ERROR', details = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

let authTokenGetter = () => {
  try {
    return localStorage.getItem('bookngo_token');
  } catch {
    return null;
  }
};

let unauthorizedListener = null;

/**
 * Configure dynamic token retriever if needed.
 */
export function setAuthTokenGetter(fn) {
  authTokenGetter = fn;
}

/**
 * Register a listener for 401 Unauthorized responses to trigger session expiration.
 */
export function onUnauthorized(callback) {
  unauthorizedListener = callback;
  return () => {
    unauthorizedListener = null;
  };
}

/**
 * Core HTTP request execution via native fetch.
 */
export async function request(path, options = {}) {
  const {
    method = 'GET',
    headers = {},
    body,
    signal,
    ...restOptions
  } = options;

  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const url = `${API_BASE_URL}${normalizedPath}`;

  const requestHeaders = new Headers(headers);

  // Default to JSON unless sending FormData
  if (!(body instanceof FormData) && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json');
  }

  // Inject Bearer token if available
  const token = typeof authTokenGetter === 'function' ? authTokenGetter() : null;
  if (token && !requestHeaders.has('Authorization')) {
    requestHeaders.set('Authorization', `Bearer ${token}`);
  }

  let response;
  try {
    response = await fetch(url, {
      method,
      headers: requestHeaders,
      body: body && typeof body === 'object' && !(body instanceof FormData) ? JSON.stringify(body) : body,
      signal,
      ...restOptions,
    });
  } catch (err) {
    if (err.name === 'AbortError') {
      throw err;
    }
    throw new ApiError(
      0,
      'Unable to connect to BookNGo services. Please check your internet connection.',
      'NETWORK_ERROR',
      err
    );
  }

  // Handle No Content response
  if (response.status === 204) {
    return null;
  }

  // Parse JSON response body if present
  let responseData;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try {
      responseData = await response.json();
    } catch {
      responseData = null;
    }
  } else {
    try {
      responseData = await response.text();
    } catch {
      responseData = null;
    }
  }

  if (!response.ok) {
    const status = response.status;
    let message = 'An unexpected error occurred.';
    let code = `HTTP_${status}`;
    let details = responseData;

    if (responseData && typeof responseData === 'object') {
      message = responseData.message || responseData.error || message;
      code = responseData.code || code;
      details = responseData.details || responseData.errors || details;
    } else if (typeof responseData === 'string' && responseData.trim().length > 0) {
      message = responseData;
    }

    // Specific status overrides
    if (status === 401) {
      code = 'UNAUTHORIZED';
      message = message || 'Session expired or authentication required.';
      if (typeof unauthorizedListener === 'function') {
        unauthorizedListener();
      }
    } else if (status === 403) {
      code = 'FORBIDDEN';
      message = message || 'You do not have permission to perform this action.';
    } else if (status === 404) {
      code = 'NOT_FOUND';
      message = message || 'The requested resource was not found.';
    } else if (status === 409) {
      code = 'CONFLICT';
      message = message || 'Resource conflict occurred. Data may have changed.';
    } else if (status >= 500) {
      code = 'SERVER_ERROR';
      message = 'BookNGo services are currently experiencing issues. Please try again shortly.';
    }

    throw new ApiError(status, message, code, details);
  }

  return responseData;
}

export const apiClient = {
  get: (path, options) => request(path, { ...options, method: 'GET' }),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' }),
};
