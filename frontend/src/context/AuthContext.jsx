import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  requestOtp as apiRequestOtp,
  verifyOtp as apiVerifyOtp,
  loginPassword as apiLoginPassword,
} from '../features/auth/api/authApi';
import { setAuthTokenGetter, onUnauthorized } from '../lib/apiClient';
import { AuthContext } from './AuthContextDefinition';

const STORAGE_KEY_TOKEN = 'bookngo_token';
const STORAGE_KEY_USER = 'bookngo_user';

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY_TOKEN);
    } catch {
      return null;
    }
  });

  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY_USER);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const [isLoading, setIsLoading] = useState(false);

  // Keep apiClient token getter in sync
  useEffect(() => {
    setAuthTokenGetter(() => token);
  }, [token]);

  // Clean logout handler
  const logout = useCallback(() => {
    try {
      localStorage.removeItem(STORAGE_KEY_TOKEN);
      localStorage.removeItem(STORAGE_KEY_USER);
    } catch {
      // ignore storage errors
    }
    setToken(null);
    setUser(null);
  }, []);

  // Listen to 401 Unauthorized events from apiClient
  useEffect(() => {
    const unsubscribe = onUnauthorized(() => {
      logout();
    });
    return unsubscribe;
  }, [logout]);

  /**
   * Request OTP for a customer phone number.
   */
  const requestOtp = useCallback(async (phoneNumber) => {
    return apiRequestOtp(phoneNumber);
  }, []);

  /**
   * Verify OTP and store returned session token & user profile.
   */
  const verifyOtp = useCallback(async (phoneNumber, otpCode) => {
    setIsLoading(true);
    try {
      const result = await apiVerifyOtp(phoneNumber, otpCode);
      if (result && result.token) {
        setToken(result.token);
        setUser(result.user || null);
        try {
          localStorage.setItem(STORAGE_KEY_TOKEN, result.token);
          if (result.user) {
            localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(result.user));
          }
        } catch {
          // ignore storage error
        }
      }
      return result;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Username & Password login for Theatre Operators & Admins.
   */
  const loginPassword = useCallback(async (username, password) => {
    setIsLoading(true);
    try {
      const result = await apiLoginPassword(username, password);
      if (result && result.token) {
        setToken(result.token);
        setUser(result.user || null);
        try {
          localStorage.setItem(STORAGE_KEY_TOKEN, result.token);
          if (result.user) {
            localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(result.user));
          }
        } catch {
          // ignore storage error
        }
      }
      return result;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token),
      isCustomer: user?.roles?.includes('CUSTOMER'),
      isOperator: user?.roles?.includes('THEATRE_OPERATOR'),
      isAdmin: user?.roles?.includes('ADMIN'),
      isLoading,
      requestOtp,
      verifyOtp,
      loginPassword,
      logout,
    }),
    [user, token, isLoading, requestOtp, verifyOtp, loginPassword, logout]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export default AuthProvider;
