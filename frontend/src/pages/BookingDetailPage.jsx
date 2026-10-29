import { useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBookingById, cancelBooking } from '../features/booking/api/bookingApi';
import {
  getShowById,
  getTheatres,
  getScreensByTheatre,
  getCancellationPolicyByShow,
} from '../features/shows/api/showsAndTheatresApi';
import { getMovieById } from '../features/movies/api/moviesApi';
import { getRefundStatus } from '../features/payment/api/paymentApi';
import { useToast } from '../context/useToast';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { LoadingState } from '../components/ui/LoadingState';
import { ErrorState } from '../components/ui/ErrorState';
import {
  ArrowLeft,
  Ticket as TicketIcon,
  Calendar,
  Clock,
  MapPin,
  Film,
  AlertTriangle,
  XCircle,
  ShieldCheck,
  RotateCcw,
  Receipt,
  Info,
} from 'lucide-react';

export function BookingDetailPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [currentTime] = useState(() => Date.now());

  // 1. Fetch Booking
  const {
    data: booking,
    isLoading: isBookingLoading,
    isError: isBookingError,
    error: bookingError,
    refetch: refetchBooking,
  } = useQuery({
    queryKey: ['booking', bookingId],
    queryFn: () => getBookingById(bookingId),
    enabled: Boolean(bookingId),
  });

  // 2. Fetch Show
  const { data: show } = useQuery({
    queryKey: ['show', booking?.showId],
    queryFn: () => getShowById(booking.showId),
    enabled: Boolean(booking?.showId),
  });

  // 3. Fetch Movie
  const { data: movie } = useQuery({
    queryKey: ['movie', show?.movieId],
    queryFn: () => getMovieById(show.movieId),
    enabled: Boolean(show?.movieId),
  });

  // 4. Fetch Theatres
  const { data: theatres = [] } = useQuery({
    queryKey: ['theatres'],
    queryFn: () => getTheatres(),
  });
  const theatre = theatres.find((t) => t.theatreId === show?.theatreId);

  // 5. Fetch Screen
  const { data: screens = [] } = useQuery({
    queryKey: ['theatreScreens', show?.theatreId],
    queryFn: () => getScreensByTheatre(show.theatreId),
    enabled: Boolean(show?.theatreId),
  });
  const screen = screens.find((s) => s.screenId === show?.screenId);

  // 6. Fetch Show Cancellation Policy
  const { data: cancellationPolicy } = useQuery({
    queryKey: ['cancellationPolicy', booking?.showId],
    queryFn: () => getCancellationPolicyByShow(booking.showId),
    enabled: Boolean(booking?.showId),
  });

  // 7. Fetch Refund Status if cancelled
  const isCancelled = booking?.status === 'CANCELLED';
  const { data: refundData } = useQuery({
    queryKey: ['refundStatus', booking?.paymentId],
    queryFn: () => getRefundStatus(booking.paymentId),
    enabled: Boolean(isCancelled && booking?.paymentId),
  });

  // Cancel Booking Mutation
  const cancelMutation = useMutation({
    mutationFn: () => cancelBooking(bookingId),
    onSuccess: () => {
      toast.success('Booking cancelled successfully. Refund initiated.');
      setCancelModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['booking', bookingId] });
      queryClient.invalidateQueries({ queryKey: ['myBookings'] });
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to cancel booking.');
    },
  });

  if (isBookingLoading) {
    return (
      <div className="min-h-[85vh] flex items-center justify-center bg-[#0A0D14]">
        <LoadingState message="Loading reservation details..." />
      </div>
    );
  }

  if (isBookingError || !booking) {
    return (
      <div className="min-h-[85vh] flex items-center justify-center bg-[#0A0D14] p-4">
        <ErrorState
          title="Reservation Not Found"
          message={bookingError?.message || 'Unable to retrieve booking details.'}
          onRetry={refetchBooking}
        />
      </div>
    );
  }

  const isConfirmed = booking.status === 'CONFIRMED' || booking.status === 'TICKET_ISSUED';

  // Check Cancellation Eligibility
  const deadline = cancellationPolicy?.cancellationDeadline
    ? new Date(cancellationPolicy.cancellationDeadline)
    : null;
  const isPastDeadline = deadline ? currentTime > deadline.getTime() : false;
  const isNonCancellable = cancellationPolicy?.policyType === 'NON_CANCELLABLE';
  const isEligibleForCancellation = isConfirmed && !isNonCancellable && !isPastDeadline;

  // Format Show Date & Time
  const showDateTime = show?.startTime ? new Date(show.startTime) : null;
  const formattedDate = showDateTime
    ? showDateTime.toLocaleDateString('en-IN', {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      })
    : 'Scheduled Date';

  const formattedTime = showDateTime
    ? showDateTime.toLocaleTimeString('en-IN', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
      })
    : 'Scheduled Time';

  const formattedDeadline = deadline
    ? deadline.toLocaleString('en-IN', {
        dateStyle: 'medium',
        timeStyle: 'short',
      })
    : null;

  const moviePoster =
    movie?.posterUrl || (movie?.title ? `/posters/${encodeURIComponent(movie.title)}.jpg` : null);

  return (
    <div className="min-h-[90vh] bg-[#0A0D14] text-white py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Back Link */}
        <div>
          <button
            type="button"
            onClick={() => navigate('/history')}
            className="inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-white transition"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to My Bookings</span>
          </button>
        </div>

        {/* Top Header Card */}
        <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 shadow-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2.5 mb-1.5">
              <Badge
                variant={isConfirmed ? 'success' : isCancelled ? 'danger' : 'warning'}
                className="text-xs font-bold"
              >
                {booking.status}
              </Badge>
              <span className="font-mono text-xs text-zinc-400">
                ID: {booking.bookingId}
              </span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              {movie?.title || 'Cinema Experience Booking'}
            </h1>
          </div>

          {/* Quick Actions */}
          <div className="flex items-center gap-3">
            {isConfirmed && (
              <Button
                variant="primary"
                onClick={() => navigate(`/bookings/${bookingId}/ticket`)}
                className="gap-2 bg-red-600 hover:bg-red-500 shadow-md shadow-red-900/30"
              >
                <TicketIcon className="w-4 h-4" />
                <span>View Digital Pass</span>
              </Button>
            )}
          </div>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column (2 Cols): Movie, Venue & Seats Details */}
          <div className="lg:col-span-2 space-y-6">
            {/* Show Details Card */}
            <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-5">
              <h2 className="text-base font-semibold text-white border-b border-white/10 pb-3 flex items-center gap-2">
                <Film className="w-4 h-4 text-red-500" />
                <span>Experience Details</span>
              </h2>

              <div className="flex gap-4 items-start">
                {moviePoster && (
                  <img
                    src={moviePoster}
                    alt={movie?.title || 'Movie'}
                    className="w-20 h-28 object-cover rounded-xl border border-white/10 shrink-0"
                    onError={(e) => {
                      e.currentTarget.style.display = 'none';
                    }}
                  />
                )}
                <div className="space-y-1.5 flex-1 min-w-0">
                  <h3 className="text-lg font-bold text-white truncate">
                    {movie?.title || 'Movie Show'}
                  </h3>
                  <div className="flex items-center gap-1.5 text-xs text-zinc-400">
                    <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0" />
                    <span>
                      {theatre?.name || 'Multiplex'}, {theatre?.city || 'Vijayawada'}
                    </span>
                  </div>
                  <p className="text-xs text-zinc-500">
                    {screen?.screenName || 'Screen 1'} &bull; {show?.format || '2D'} &bull;{' '}
                    {show?.language || 'Original'}
                  </p>
                </div>
              </div>

              {/* Schedule Info */}
              <div className="grid grid-cols-2 gap-3 pt-2">
                <div className="rounded-xl bg-white/[0.03] border border-white/10 p-3">
                  <span className="text-xs text-zinc-500 flex items-center gap-1 mb-1">
                    <Calendar className="w-3.5 h-3.5 text-red-400" />
                    Date
                  </span>
                  <p className="text-sm font-semibold text-white">{formattedDate}</p>
                </div>
                <div className="rounded-xl bg-white/[0.03] border border-white/10 p-3">
                  <span className="text-xs text-zinc-500 flex items-center gap-1 mb-1">
                    <Clock className="w-3.5 h-3.5 text-red-400" />
                    Showtime
                  </span>
                  <p className="text-sm font-semibold text-white">{formattedTime}</p>
                </div>
              </div>
            </div>

            {/* Reserved Seats List */}
            <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-4">
              <h2 className="text-base font-semibold text-white border-b border-white/10 pb-3 flex items-center justify-between">
                <span>Reserved Seats ({booking.bookingSeats?.length || 0})</span>
                <span className="text-xs text-zinc-400 font-normal">Allocated</span>
              </h2>

              <div className="divide-y divide-white/5">
                {(booking.bookingSeats || []).map((seat, index) => (
                  <div key={seat.bookingSeatId || index} className="py-2.5 flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2.5">
                      <span className="px-2.5 py-1 rounded bg-red-600/20 border border-red-500/40 text-red-400 font-bold font-mono text-xs">
                        {seat.seatLabelSnapshot || `Seat ${index + 1}`}
                      </span>
                      <span className="text-zinc-400 text-xs uppercase tracking-wider">
                        {seat.seatCategorySnapshot || 'Standard'}
                      </span>
                    </div>
                    <span className="font-mono text-zinc-200">
                      {booking.currency || 'INR'} {Number(seat.unitPriceSnapshot || 0).toLocaleString('en-IN')}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* Cancellation Policy Section */}
            <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-4">
              <h2 className="text-base font-semibold text-white border-b border-white/10 pb-3 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                <span>Show Cancellation Policy</span>
              </h2>

              <div className="text-xs text-zinc-400 space-y-2">
                <div className="flex items-center justify-between">
                  <span>Policy Type:</span>
                  <Badge variant={isNonCancellable ? 'danger' : 'success'}>
                    {cancellationPolicy?.policyType || 'STANDARD'}
                  </Badge>
                </div>

                {formattedDeadline && (
                  <div className="flex items-center justify-between">
                    <span>Cancellation Deadline:</span>
                    <span className="text-zinc-200 font-medium">{formattedDeadline}</span>
                  </div>
                )}

                <p className="text-zinc-500 pt-2 leading-relaxed">
                  {isNonCancellable
                    ? 'Tickets for this special engagement or screening format cannot be cancelled or exchanged.'
                    : `Cancellations are accepted up to the show deadline. Upon cancellation, your payment will be refunded via your original payment channel.`}
                </p>
              </div>

              {/* Cancellation Action Button */}
              {isEligibleForCancellation && (
                <div className="pt-2">
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => setCancelModalOpen(true)}
                    className="w-full sm:w-auto gap-2"
                  >
                    <XCircle className="w-4 h-4" />
                    <span>Cancel Reservation &amp; Refund</span>
                  </Button>
                </div>
              )}

              {isConfirmed && !isEligibleForCancellation && (
                <div className="p-3 rounded-xl bg-white/[0.03] border border-white/10 text-xs text-zinc-400 flex items-center gap-2">
                  <Info className="w-4 h-4 text-amber-400 shrink-0" />
                  <span>
                    {isNonCancellable
                      ? 'This show is non-cancellable.'
                      : 'The cancellation deadline for this show has passed.'}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Payment & Price Summary */}
          <div className="space-y-6">
            {/* Price Breakdown */}
            <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-4">
              <h2 className="text-base font-semibold text-white border-b border-white/10 pb-3 flex items-center gap-2">
                <Receipt className="w-4 h-4 text-zinc-400" />
                <span>Payment Summary</span>
              </h2>

              <div className="space-y-2 text-sm text-zinc-400">
                <div className="flex justify-between">
                  <span>Tickets Subtotal</span>
                  <span className="text-white font-mono">
                    {booking.currency || 'INR'} {Number(booking.totalAmount).toLocaleString('en-IN')}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Convenience Fee</span>
                  <span className="text-emerald-400 font-medium">Included</span>
                </div>
                <div className="border-t border-white/10 pt-3 flex justify-between items-center text-base font-bold text-white">
                  <span>Total Paid</span>
                  <span className="text-red-400 font-mono">
                    {booking.currency || 'INR'} {Number(booking.totalAmount).toLocaleString('en-IN')}
                  </span>
                </div>
              </div>

              {booking.paymentId && (
                <div className="pt-3 border-t border-white/10 text-[11px] text-zinc-500 space-y-1">
                  <span className="block">Payment ID:</span>
                  <span className="font-mono text-zinc-400 break-all">{booking.paymentId}</span>
                </div>
              )}
            </div>

            {/* Refund Status Card (if cancelled) */}
            {isCancelled && (
              <div className="rounded-2xl border border-rose-500/30 bg-rose-950/20 p-5 space-y-3">
                <div className="flex items-center gap-2 text-rose-300 font-semibold text-sm">
                  <RotateCcw className="w-4 h-4" />
                  <span>Refund Initiated</span>
                </div>
                <p className="text-xs text-rose-200/80 leading-relaxed">
                  Your cancellation request has processed. The refund has been submitted to your original
                  payment source.
                </p>
                {refundData && (
                  <div className="pt-2 text-xs space-y-1 text-zinc-300">
                    <div className="flex justify-between">
                      <span>Refund Status:</span>
                      <span className="font-bold text-emerald-400">{refundData.status || 'PROCESSED'}</span>
                    </div>
                    {refundData.amount && (
                      <div className="flex justify-between">
                        <span>Refund Amount:</span>
                        <span className="font-mono text-white">
                          INR {Number(refundData.amount).toLocaleString('en-IN')}
                        </span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Cancellation Confirmation Modal */}
        <Modal
          isOpen={cancelModalOpen}
          onClose={() => setCancelModalOpen(false)}
          title="Confirm Ticket Cancellation"
        >
          <div className="space-y-4">
            <div className="p-3 rounded-xl bg-rose-950/40 border border-rose-500/30 text-rose-200 text-xs flex items-center gap-2.5">
              <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
              <span>
                Cancelling will release your reserved seats back to cinema inventory. This action cannot be undone.
              </span>
            </div>

            <p className="text-sm text-zinc-300">
              Are you sure you want to cancel your reservation for <strong>{movie?.title}</strong>? A refund of{' '}
              <strong className="text-white font-mono">
                {booking.currency || 'INR'} {Number(booking.totalAmount).toLocaleString('en-IN')}
              </strong>{' '}
              will be initiated to your payment provider.
            </p>

            <div className="flex justify-end gap-3 pt-3">
              <Button
                variant="outline"
                onClick={() => setCancelModalOpen(false)}
                disabled={cancelMutation.isPending}
              >
                Keep Booking
              </Button>
              <Button
                variant="danger"
                onClick={() => cancelMutation.mutate()}
                isLoading={cancelMutation.isPending}
              >
                Yes, Cancel Reservation
              </Button>
            </div>
          </div>
        </Modal>
      </div>
    </div>
  );
}

export default BookingDetailPage;
