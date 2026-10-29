import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Clock,
  MapPin,
  Calendar,
  ShieldCheck,
  AlertTriangle,
  Film,
  Lock,
} from 'lucide-react';
import { useToast } from '../context/useToast';
import { getBookingById } from '../features/booking/api/bookingApi';
import { fetchShowById } from '../features/shows/api/showsAndTheatresApi';
import { fetchTheatresByCity } from '../features/shows/api/showsAndTheatresApi';
import { useMovieDetail } from '../features/movies/hooks/useMovieDetail';
import { initiatePayment } from '../features/payment/api/paymentApi';
import { releaseHold } from '../features/seats/api/seatsApi';
import { getMoviePosterUrl } from '../features/movies/lib/moviePosters';
import { ProgressIndicator } from '../components/ui/ProgressIndicator';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { LoadingState } from '../components/ui/LoadingState';
import { ErrorState } from '../components/ui/ErrorState';

export function CheckoutPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const [providerName, setProviderName] = useState('SIM_SUCCESS');
  const [expiredModalOpen, setExpiredModalOpen] = useState(false);
  const [secondsRemaining, setSecondsRemaining] = useState(null);

  // Retrieve hold response passed via navigation state if available
  const initialHoldResponse = location.state?.holdResponse || null;

  // 1. Fetch Booking Details
  const {
    data: booking,
    isLoading: loadingBooking,
    error: bookingError,
    refetch: refetchBooking,
  } = useQuery({
    queryKey: ['booking', bookingId],
    queryFn: ({ signal }) => getBookingById(bookingId, { signal }),
    enabled: Boolean(bookingId),
    refetchInterval: (query) => {
      // Don't refetch if booking is already terminal
      const status = query.state.data?.status;
      return status === 'CONFIRMED' || status === 'CANCELLED' || status === 'EXPIRED'
        ? false
        : 8000;
    },
  });

  const showId = booking?.showId;

  // 2. Fetch Show Details
  const { data: show } = useQuery({
    queryKey: ['show', showId],
    queryFn: ({ signal }) => fetchShowById(showId, { signal }),
    enabled: Boolean(showId),
  });

  // 3. Fetch Movie Details
  const { data: movie } = useMovieDetail(show?.movieId);

  // 4. Fetch Theatres
  const { data: theatres = [] } = useQuery({
    queryKey: ['theatres'],
    queryFn: ({ signal }) => fetchTheatresByCity('', { signal }),
    staleTime: 5 * 60 * 1000,
  });

  const currentTheatre = useMemo(() => {
    if (!show?.theatreId) return null;
    return theatres.find((t) => t.theatreId === show.theatreId) || null;
  }, [theatres, show]);

  // Server-authoritative hold expiration
  const normalExpiresAt = useMemo(() => {
    if (initialHoldResponse?.normalExpiresAt) {
      return initialHoldResponse.normalExpiresAt;
    }
    // If not in state, estimate 5 mins from booking createdAt
    if (booking?.createdAt) {
      const created = new Date(booking.createdAt).getTime();
      return new Date(created + 5 * 60 * 1000).toISOString();
    }
    return null;
  }, [initialHoldResponse, booking]);

  // Hold Countdown Timer
  useEffect(() => {
    if (!normalExpiresAt) return;

    const calculateRemaining = () => {
      const diffMs = new Date(normalExpiresAt).getTime() - Date.now();
      const secs = Math.max(0, Math.floor(diffMs / 1000));
      setSecondsRemaining(secs);

      if (secs <= 0) {
        setExpiredModalOpen(true);
      }
    };

    calculateRemaining();
    const interval = setInterval(calculateRemaining, 1000);
    return () => clearInterval(interval);
  }, [normalExpiresAt]);

  const formattedTimer = useMemo(() => {
    if (secondsRemaining === null) return '05:00';
    const m = Math.floor(secondsRemaining / 60);
    const s = secondsRemaining % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }, [secondsRemaining]);

  const isTimerUrgent = secondsRemaining !== null && secondsRemaining < 60;

  // Payment Initiation Mutation
  const paymentMutation = useMutation({
    mutationFn: async () => {
      const idempotencyKey = crypto.randomUUID();
      return initiatePayment({
        bookingId,
        providerName,
        idempotencyKey,
      });
    },
    onSuccess: (paymentResponse) => {
      navigate(`/checkout/${bookingId}/processing?paymentId=${paymentResponse.paymentId}`, {
        state: { paymentResponse },
      });
    },
    onError: (err) => {
      if (err.status === 409) {
        toast.error('Seat hold has expired. Please select your seats again.');
        setExpiredModalOpen(true);
      } else {
        toast.error(err.message || 'Payment initiation failed. Please try again.');
      }
    },
  });

  // Release Hold Mutation
  const releaseMutation = useMutation({
    mutationFn: async () => {
      const holdId = initialHoldResponse?.holdId;
      if (holdId) {
        await releaseHold(holdId);
      }
    },
    onSettled: () => {
      toast.info('Seats released. No charges were made.');
      navigate(showId ? `/shows/${showId}/seats` : '/movies');
    },
  });

  const handlePayClick = (e) => {
    e.preventDefault();
    if (secondsRemaining !== null && secondsRemaining <= 0) {
      setExpiredModalOpen(true);
      return;
    }
    paymentMutation.mutate();
  };

  const resolvedPosterUrl = movie ? getMoviePosterUrl(movie) : null;

  if (loadingBooking) {
    return (
      <div className="w-full min-h-screen bg-[#0B0F17] flex items-center justify-center">
        <LoadingState
          title="Preparing Checkout..."
          message="Loading booking details and verifying authoritative pricing"
          variant="full"
        />
      </div>
    );
  }

  if (bookingError || !booking) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <ErrorState
          title="Booking Not Found"
          message={bookingError?.message || 'Unable to load booking details.'}
          onRetry={refetchBooking}
        />
      </div>
    );
  }

  const bookingSeats = Array.isArray(booking.bookingSeats) ? booking.bookingSeats : [];

  return (
    <div className="w-full min-h-screen bg-[#0B0F17] text-gray-100 flex flex-col pb-24">
      {/* 1. PROGRESS WIZARD */}
      <ProgressIndicator currentStep={3} />

      {/* 2. HOLD COUNTDOWN WARNING BANNER */}
      <section
        role="region"
        aria-label="Hold Expiration Timer"
        className={`w-full py-3 px-4 border-b transition-colors select-none ${
          isTimerUrgent
            ? 'bg-red-950/80 border-red-500/50 text-red-200 animate-pulse'
            : 'bg-amber-950/40 border-amber-500/30 text-amber-200'
        }`}
      >
        <div className="max-w-7xl mx-auto flex items-center justify-between text-xs sm:text-sm font-semibold">
          <div className="flex items-center gap-2">
            <Clock className={`w-4 h-4 ${isTimerUrgent ? 'text-red-400' : 'text-amber-400'}`} />
            <span>Seats held for checkout. Complete payment before the timer expires.</span>
          </div>

          <div
            className={`font-mono font-black text-sm sm:text-base px-2.5 py-0.5 rounded-lg border ${
              isTimerUrgent
                ? 'bg-red-900 border-red-400 text-white'
                : 'bg-amber-950 border-amber-500/40 text-amber-300'
            }`}
          >
            {formattedTimer}
          </div>
        </div>
      </section>

      {/* 3. MAIN CHECKOUT CONTENT */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Column: Order Summary & Selected Seats (8 cols) */}
          <div className="lg:col-span-7 space-y-6">
            {/* Movie & Showtime Header Card */}
            <div className="p-6 bg-slate-900/80 border border-slate-800 rounded-2xl shadow-xl flex gap-5">
              {resolvedPosterUrl ? (
                <img
                  src={resolvedPosterUrl}
                  alt={movie?.title || ''}
                  className="w-20 sm:w-24 aspect-[2/3] rounded-xl object-cover border border-slate-800 shrink-0 shadow-md"
                />
              ) : (
                <div className="w-20 sm:w-24 aspect-[2/3] rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center shrink-0 text-slate-500">
                  <Film className="w-8 h-8" />
                </div>
              )}

              <div className="flex-1 space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <Badge variant="brand" size="xs">
                    {movie?.language || 'Feature'}
                  </Badge>
                  {show?.showFormat && (
                    <Badge variant="default" size="xs">
                      {show.showFormat}
                    </Badge>
                  )}
                </div>

                <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight leading-snug font-heading">
                  {movie?.title || 'Movie'}
                </h2>

                <div className="flex items-center gap-1.5 text-xs text-slate-400">
                  <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0" />
                  <span className="text-slate-200 font-semibold">
                    {currentTheatre?.name || 'PVR Cinema'}
                  </span>
                  <span>({currentTheatre?.city || 'Vijayawada'})</span>
                </div>

                {show?.startsAt && (
                  <div className="flex items-center gap-2 text-xs text-slate-300 font-medium">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>
                      {new Date(show.startsAt).toLocaleDateString('en-IN', {
                        weekday: 'short',
                        day: 'numeric',
                        month: 'short',
                      })}
                    </span>
                    <span className="text-slate-600">•</span>
                    <span className="text-red-400 font-bold">
                      {new Date(show.startsAt).toLocaleTimeString('en-US', {
                        hour: 'numeric',
                        minute: '2-digit',
                        hour12: true,
                      })}
                    </span>
                  </div>
                )}
              </div>
            </div>

            {/* Allocated Seats Breakdown */}
            <div className="p-6 bg-slate-900/80 border border-slate-800 rounded-2xl shadow-xl space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800/80">
                <h3 className="text-sm font-bold uppercase tracking-wider text-slate-300">
                  Allocated Seats ({bookingSeats.length})
                </h3>
                <span className="text-xs text-emerald-400 font-semibold flex items-center gap-1">
                  <ShieldCheck className="w-4 h-4" />
                  Lock Authoritative
                </span>
              </div>

              <div className="space-y-2.5">
                {bookingSeats.map((seat) => (
                  <div
                    key={seat.bookingSeatId}
                    className="flex items-center justify-between p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 text-xs"
                  >
                    <div className="flex items-center gap-3">
                      <span className="w-10 h-7 rounded-lg bg-red-600/20 border border-red-500/40 text-red-400 font-black flex items-center justify-center font-mono">
                        {seat.seatLabelSnapshot}
                      </span>
                      <span className="text-slate-300 font-medium">
                        {seat.seatCategorySnapshot} Tier
                      </span>
                    </div>

                    <div className="font-bold text-white text-sm">
                      ₹{seat.unitPriceSnapshot}
                    </div>
                  </div>
                ))}
              </div>

              <div className="p-3 rounded-xl bg-slate-950/40 border border-slate-800 text-[11px] text-slate-400 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>
                  Free cancellation available up to 2 hours before showtime per cinema policy.
                </span>
              </div>
            </div>
          </div>

          {/* Right Column: Payment Simulation & Authoritative Price Breakdown (5 cols) */}
          <div className="lg:col-span-5 space-y-6">
            {/* Price Card */}
            <div className="p-6 bg-slate-900/90 border border-slate-800 rounded-2xl shadow-xl space-y-5">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 pb-2 border-b border-slate-800/80">
                Payment Breakdown
              </h3>

              <div className="space-y-2.5 text-xs text-slate-300">
                <div className="flex justify-between">
                  <span>Tickets Subtotal</span>
                  <span className="font-semibold text-white">
                    ₹{booking.totalAmount}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Convenience Fee &amp; GST</span>
                  <span className="text-emerald-400 font-medium">Included</span>
                </div>
              </div>

              {/* Total Payable */}
              <div className="pt-4 border-t border-slate-800 flex items-baseline justify-between">
                <div>
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-400 block">
                    Total Payable
                  </span>
                  <span className="text-[10px] text-slate-500">
                    Authoritative Backend Amount
                  </span>
                </div>

                <div className="text-2xl sm:text-3xl font-black text-white tracking-tight">
                  <span className="text-red-500 mr-0.5">₹</span>
                  {booking.totalAmount}
                </div>
              </div>

              {/* Payment Simulator Selection */}
              <div className="pt-4 border-t border-slate-800 space-y-3">
                <label
                  htmlFor="payment-simulator"
                  className="block text-xs font-bold uppercase tracking-wider text-slate-400"
                >
                  Payment Method Simulator
                </label>

                <div className="space-y-2 text-xs">
                  {[
                    {
                      id: 'SIM_SUCCESS',
                      name: 'Instant Net Banking / UPI (Successful)',
                      desc: 'Exercises the standard verified payment success path',
                      badge: 'Recommended',
                      badgeVariant: 'success',
                    },
                    {
                      id: 'SIM_FAILURE',
                      name: 'Simulated Bank Decline',
                      desc: 'Simulates payment refusal or bank rejection',
                      badge: 'Test Failure',
                      badgeVariant: 'danger',
                    },
                    {
                      id: 'SIM_CANCELLED',
                      name: 'Simulate User Cancellation',
                      desc: 'Simulates customer cancelling at bank gateway',
                      badge: 'Test Cancel',
                      badgeVariant: 'warning',
                    },
                    {
                      id: 'SIM_UNKNOWN',
                      name: 'Simulate Gateway Timeout / Unknown',
                      desc: 'Exercises the 2-minute grace reconciliation state',
                      badge: 'Test Recovery',
                      badgeVariant: 'info',
                    },
                  ].map((option) => (
                    <label
                      key={option.id}
                      className={`flex items-start gap-3 p-3.5 rounded-xl border cursor-pointer transition-all ${
                        providerName === option.id
                          ? 'bg-red-950/20 border-red-500/50 shadow-sm'
                          : 'bg-slate-950/50 border-slate-800 hover:border-slate-700'
                      }`}
                    >
                      <input
                        type="radio"
                        name="payment_provider"
                        value={option.id}
                        checked={providerName === option.id}
                        onChange={(e) => setProviderName(e.target.value)}
                        className="mt-1 text-red-600 focus:ring-red-500 cursor-pointer"
                      />
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-white text-xs">
                            {option.name}
                          </span>
                          <Badge variant={option.badgeVariant} size="xs">
                            {option.badge}
                          </Badge>
                        </div>
                        <p className="text-[11px] text-slate-400 mt-0.5">
                          {option.desc}
                        </p>
                      </div>
                    </label>
                  ))}
                </div>
              </div>

              {/* Pay Button */}
              <div className="pt-2 space-y-3">
                <Button
                  variant="primary"
                  size="lg"
                  disabled={paymentMutation.isPending || (secondsRemaining !== null && secondsRemaining <= 0)}
                  isLoading={paymentMutation.isPending}
                  onClick={handlePayClick}
                  leftIcon={<Lock className="w-4 h-4" />}
                  className="w-full text-base font-bold py-3.5 rounded-xl shadow-2xl shadow-red-600/30"
                >
                  {paymentMutation.isPending
                    ? 'Contacting Payment Gateway...'
                    : `Pay ₹${booking.totalAmount}`}
                </Button>

                <Button
                  variant="ghost"
                  size="sm"
                  disabled={paymentMutation.isPending || releaseMutation.isPending}
                  onClick={() => releaseMutation.mutate()}
                  className="w-full text-xs text-slate-400 hover:text-red-400"
                >
                  Cancel &amp; Release Seats
                </Button>
              </div>

              <div className="text-[10px] text-center text-slate-500 flex items-center justify-center gap-1.5">
                <Lock className="w-3 h-3 text-emerald-400" />
                <span>256-Bit Idempotent Transaction Encryption</span>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* 4. HOLD EXPIRED MODAL */}
      <Modal
        isOpen={expiredModalOpen}
        onClose={() => {}}
        title="Seat Hold Expired"
        showCloseButton={false}
        description="Your 5-minute reservation window has elapsed."
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 p-4 rounded-xl bg-rose-950/30 border border-rose-500/40 text-rose-200 text-xs leading-relaxed">
            <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
            <div>
              To ensure fair availability for all cinema guests, unpurchased seat holds expire after 5 minutes.
              Your seats have been returned to available inventory.
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                navigate(showId ? `/shows/${showId}/seats` : '/movies');
              }}
            >
              Choose Seats Again
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default CheckoutPage;
