import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router';
import { useAuth } from '../context/useAuth';
import { Button } from '../components/ui/Button';
import { Shield, Smartphone, AlertCircle } from 'lucide-react';

export function LoginPage() {
  const { isAuthenticated, requestOtp, verifyOtp, loginPassword, isLoading, isOperator, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnUrl = searchParams.get('returnUrl') || '/';

  // Mode: 'CUSTOMER' | 'STAFF'
  const [loginMode, setLoginMode] = useState('CUSTOMER');

  // Customer state
  const [step, setStep] = useState('PHONE'); // 'PHONE' | 'OTP'
  const [phoneNumber, setPhoneNumber] = useState('');
  const [otpCode, setOtpCode] = useState('');

  // Staff state
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const [errorMessage, setErrorMessage] = useState('');
  const [resendCooldown, setResendCooldown] = useState(0);

  // If already authenticated, redirect
  useEffect(() => {
    if (isAuthenticated) {
      if (returnUrl && returnUrl !== '/') {
        navigate(returnUrl, { replace: true });
      } else if (isAdmin) {
        navigate('/admin', { replace: true });
      } else if (isOperator) {
        navigate('/operator', { replace: true });
      } else {
        navigate('/', { replace: true });
      }
    }
  }, [isAuthenticated, isAdmin, isOperator, navigate, returnUrl]);

  // Resend countdown timer
  useEffect(() => {
    if (resendCooldown <= 0) return;
    const interval = setInterval(() => {
      setResendCooldown((c) => c - 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [resendCooldown]);

  const handlePhoneSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    // Ensure phone has leading + and valid digits
    let cleaned = phoneNumber.trim();
    if (!cleaned.startsWith('+')) {
      if (/^\d{10}$/.test(cleaned)) {
        cleaned = `+91${cleaned}`;
      } else {
        setErrorMessage('Please enter a valid phone number (e.g. +919876543210 or 10-digit number)');
        return;
      }
    }

    try {
      await requestOtp(cleaned);
      setPhoneNumber(cleaned);
      setStep('OTP');
      setResendCooldown(60);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to request OTP. Please try again.');
    }
  };

  const handleOtpSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!/^\d{6}$/.test(otpCode.trim())) {
      setErrorMessage('Please enter the 6-digit verification code.');
      return;
    }

    try {
      await verifyOtp(phoneNumber, otpCode.trim());
      navigate(returnUrl, { replace: true });
    } catch (err) {
      setErrorMessage(err.message || 'Invalid or expired OTP. Please try again.');
    }
  };

  const handleStaffSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!username.trim() || !password) {
      setErrorMessage('Please provide both username and password.');
      return;
    }

    try {
      const res = await loginPassword(username.trim(), password);
      const roles = res?.user?.roles || [];
      if (roles.includes('ADMIN')) {
        navigate('/admin', { replace: true });
      } else if (roles.includes('THEATRE_OPERATOR')) {
        navigate('/operator', { replace: true });
      } else {
        navigate(returnUrl, { replace: true });
      }
    } catch (err) {
      setErrorMessage(err.message || 'Authentication failed. Please verify credentials.');
    }
  };

  const handleResend = async () => {
    if (resendCooldown > 0) return;
    setErrorMessage('');
    try {
      await requestOtp(phoneNumber);
      setResendCooldown(60);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to resend code.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-950/85 px-4 py-6 backdrop-blur-md sm:py-10">
      <div className="relative my-auto w-full max-w-[580px] overflow-hidden rounded-[26px] border border-white/20 bg-[#121620] text-white shadow-[0_28px_90px_rgba(0,0,0,0.7)] backdrop-blur-2xl">
        {/* Top Header */}
        <div className="relative overflow-hidden border-b border-white/10 bg-white/[0.04] px-6 pb-8 pt-8 text-center sm:px-10">
          <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_0%,rgba(239,68,68,.15),transparent_40%)]" />

          <button
            type="button"
            aria-label="Close login"
            onClick={() => navigate('/')}
            className="absolute right-5 top-5 z-10 rounded-full p-2 text-white/60 transition hover:bg-white/10 hover:text-white"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>

          <img
            src="/bookngofavicon-square.png"
            alt="BookNGo"
            className="relative mx-auto h-20 w-20 rounded-2xl object-cover shadow-xl ring-2 ring-red-500/40"
          />

          <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight mt-3">
            {loginMode === 'CUSTOMER' ? 'Welcome to BookNGo' : 'Cinema Staff & Operator Sign In'}
          </h2>

          <p className="relative mx-auto mt-1 max-w-sm text-xs sm:text-sm text-zinc-400">
            {loginMode === 'CUSTOMER'
              ? step === 'PHONE'
                ? 'Instant access to movie tickets, reservations, and boarding passes.'
                : `We sent a 6-digit code to ${phoneNumber}`
              : 'Enter authorized operator credentials for theatre screen management.'}
          </p>

          {/* Mode Switcher Tabs */}
          <div className="mt-5 inline-flex p-1 rounded-xl bg-black/40 border border-white/10 text-xs">
            <button
              type="button"
              onClick={() => {
                setLoginMode('CUSTOMER');
                setErrorMessage('');
              }}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-lg font-medium transition ${
                loginMode === 'CUSTOMER'
                  ? 'bg-red-600 text-white shadow-md'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Customer OTP</span>
            </button>
            <button
              type="button"
              onClick={() => {
                setLoginMode('STAFF');
                setErrorMessage('');
              }}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-lg font-medium transition ${
                loginMode === 'STAFF'
                  ? 'bg-red-600 text-white shadow-md'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              <Shield className="w-3.5 h-3.5" />
              <span>Staff / Operator</span>
            </button>
          </div>
        </div>

        {/* Form Body */}
        <div className="px-6 py-7 sm:px-10">
          {errorMessage && (
            <div className="mb-5 flex items-center gap-2.5 rounded-xl border border-rose-500/30 bg-rose-950/40 p-3.5 text-xs text-rose-200">
              <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* CUSTOMER PHONE */}
          {loginMode === 'CUSTOMER' && step === 'PHONE' && (
            <form onSubmit={handlePhoneSubmit} className="space-y-4">
              <div className="space-y-1.5">
                <label htmlFor="login-phone" className="block text-xs font-semibold uppercase tracking-wider text-zinc-400">
                  Mobile Number
                </label>
                <div className="flex gap-2">
                  <div className="flex h-12 shrink-0 items-center gap-1.5 rounded-xl border border-white/15 bg-white/[0.05] px-3.5 text-sm font-medium text-white">
                    <span>🇮🇳</span>
                    <span>+91</span>
                  </div>
                  <input
                    id="login-phone"
                    type="tel"
                    placeholder="Enter 10-digit number"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    required
                    autoFocus
                    className="h-12 min-w-0 flex-1 rounded-xl border border-white/15 bg-white/[0.05] px-4 text-sm text-white outline-none transition placeholder:text-zinc-500 focus:border-red-500/60 focus:bg-white/[0.08]"
                  />
                </div>
              </div>

              <Button
                type="submit"
                variant="primary"
                size="lg"
                className="w-full h-12 rounded-xl bg-red-600 hover:bg-red-500 font-semibold text-sm shadow-lg shadow-red-900/30"
                isLoading={isLoading}
              >
                Send Verification Code
              </Button>
            </form>
          )}

          {/* CUSTOMER OTP */}
          {loginMode === 'CUSTOMER' && step === 'OTP' && (
            <form onSubmit={handleOtpSubmit} className="space-y-4">
              <div className="space-y-1.5">
                <label htmlFor="login-otp" className="block text-xs font-semibold uppercase tracking-wider text-zinc-400">
                  Verification Code
                </label>
                <input
                  id="login-otp"
                  type="text"
                  maxLength={6}
                  placeholder="Enter 6-digit code"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                  required
                  autoFocus
                  className="h-12 w-full rounded-xl border border-white/15 bg-white/[0.05] px-4 text-center text-lg font-mono tracking-[0.4em] text-white outline-none transition placeholder:text-xs placeholder:tracking-normal placeholder:text-zinc-500 focus:border-red-500/60"
                />
              </div>

              <Button
                type="submit"
                variant="primary"
                size="lg"
                className="w-full h-12 rounded-xl bg-red-600 hover:bg-red-500 font-semibold text-sm shadow-lg shadow-red-900/30"
                isLoading={isLoading}
              >
                Verify &amp; Continue
              </Button>

              <div className="flex items-center justify-between text-xs pt-1">
                <button
                  type="button"
                  onClick={() => {
                    setStep('PHONE');
                    setOtpCode('');
                    setErrorMessage('');
                  }}
                  className="text-zinc-400 hover:text-white transition"
                >
                  Change number
                </button>
                <button
                  type="button"
                  onClick={handleResend}
                  disabled={resendCooldown > 0}
                  className="font-semibold text-red-400 hover:text-red-300 disabled:text-zinc-600"
                >
                  {resendCooldown > 0 ? `Resend in ${resendCooldown}s` : 'Resend code'}
                </button>
              </div>
            </form>
          )}

          {/* STAFF PASSWORD LOGIN */}
          {loginMode === 'STAFF' && (
            <form onSubmit={handleStaffSubmit} className="space-y-4">
              <div className="space-y-1.5">
                <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400">
                  Username / Identifier
                </label>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="e.g. operator_pvr"
                  required
                  autoFocus
                  className="h-12 w-full rounded-xl border border-white/15 bg-white/[0.05] px-4 text-sm text-white outline-none transition placeholder:text-zinc-500 focus:border-red-500/60 focus:bg-white/[0.08]"
                />
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400">
                  Password
                </label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="h-12 w-full rounded-xl border border-white/15 bg-white/[0.05] px-4 text-sm text-white outline-none transition placeholder:text-zinc-500 focus:border-red-500/60 focus:bg-white/[0.08]"
                />
              </div>

              <Button
                type="submit"
                variant="primary"
                size="lg"
                className="w-full h-12 rounded-xl bg-red-600 hover:bg-red-500 font-semibold text-sm shadow-lg shadow-red-900/30"
                isLoading={isLoading}
              >
                Sign In to Cinema Portal
              </Button>
            </form>
          )}

          <p className="mt-6 text-center text-xs text-zinc-500 leading-relaxed">
            By signing in, you agree to BookNGo{' '}
            <Link to="/" className="text-zinc-300 underline underline-offset-2 hover:text-white">
              Terms of Service
            </Link>{' '}
            and{' '}
            <Link to="/" className="text-zinc-300 underline underline-offset-2 hover:text-white">
              Privacy Policy
            </Link>
            .
          </p>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
