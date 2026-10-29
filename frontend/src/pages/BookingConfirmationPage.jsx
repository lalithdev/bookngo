import { useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { getBookingById } from '../features/booking/api/bookingApi';
import { getShowById } from '../features/shows/api/showsAndTheatresApi';
import { getMovieById } from '../features/movies/api/moviesApi';
import { getTheatres } from '../features/shows/api/showsAndTheatresApi';
import { useToast } from '../context/useToast';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { LoadingState } from '../components/ui/LoadingState';
import { ErrorState } from '../components/ui/ErrorState';
import {
  CheckCircle2,
  Ticket as TicketIcon,
  Calendar,
  Clock,
  MapPin,
  Film,
  Copy,
  Check,
  ShieldCheck,
} from 'lucide-react';

export function BookingConfirmationPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [copied, setCopied] = useState(false);

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

  // 2. Fetch Show details
  const { data: show } = useQuery({
    queryKey: ['show', booking?.showId],
    queryFn: () => getShowById(booking.showId),
    enabled: Boolean(booking?.showId),
  });

  // 3. Fetch Movie details
  const { data: movie } = useQuery({
    queryKey: ['movie', show?.movieId],
    queryFn: () => getMovieById(show.movieId),
    enabled: Boolean(show?.movieId),
  });

  // 4. Fetch Theatres to find theatre name
  const { data: theatres = [] } = useQuery({
    queryKey: ['theatres'],
    queryFn: () => getTheatres(),
  });

  const theatre = theatres.find((t) => t.theatreId === show?.theatreId);

  const handleCopyBookingId = () => {
    if (!bookingId) return;
    navigator.clipboard.writeText(bookingId);
    setCopied(true);
    toast.success('Booking ID copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
  };

  if (isBookingLoading) {
    return (
      <div className="min-h-[80vh] flex items-center justify-center bg-[#0A0D14]">
        <LoadingState message="Loading your confirmed booking details..." />
      </div>
    );
  }

  if (isBookingError || !booking) {
    return (
      <div className="min-h-[80vh] flex items-center justify-center bg-[#0A0D14] p-4">
        <ErrorState
          title="Booking Not Found"
          message={bookingError?.message || 'Unable to retrieve booking confirmation.'}
          onRetry={refetchBooking}
        />
      </div>
    );
  }

  // Format Show Date & Time
  const showDateTime = show?.startTime ? new Date(show.startTime) : null;
  const formattedDate = showDateTime
    ? showDateTime.toLocaleDateString('en-IN', {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      })
    : 'Date scheduled';

  const formattedTime = showDateTime
    ? showDateTime.toLocaleTimeString('en-IN', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
      })
    : 'Time scheduled';

  const seatLabels = (booking.bookingSeats || [])
    .map((s) => s.seatLabelSnapshot)
    .filter(Boolean)
    .join(', ');

  const moviePoster =
    movie?.posterUrl || (movie?.title ? `/posters/${encodeURIComponent(movie.title)}.jpg` : null);

  return (
    <div className="min-h-[90vh] bg-[#0A0D14] text-white py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-2xl mx-auto space-y-8">
        {/* Celebration Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-emerald-500/10 border-2 border-emerald-500/30 text-emerald-400 mb-2 shadow-[0_0_30px_rgba(16,185,129,0.2)]">
            <CheckCircle2 className="w-10 h-10 animate-bounce" />
          </div>
          <div className="inline-block">
            <span className="text-xs font-bold uppercase tracking-widest text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-3 py-1 rounded-full">
              Booking Confirmed
            </span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-white">
            You're Going to the Movies!
          </h1>
          <p className="text-sm text-zinc-400 max-w-md mx-auto">
            Your seats have been confirmed and digital passes are issued. A confirmation has been
            saved to your account.
          </p>
        </div>

        {/* Cinema Ticket Card */}
        <div className="rounded-3xl border border-white/10 bg-[#121620] overflow-hidden shadow-2xl relative">
          {/* Top Ticket Header Banner */}
          <div className="bg-gradient-to-r from-red-950/60 via-zinc-900 to-zinc-900 border-b border-white/10 p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <span className="text-xs uppercase tracking-wider text-zinc-400">Booking Reference</span>
              <div className="flex items-center gap-2 mt-1">
                <span className="font-mono text-sm sm:text-base font-bold text-white tracking-wider">
                  {bookingId}
                </span>
                <button
                  type="button"
                  onClick={handleCopyBookingId}
                  className="p-1 rounded-md text-zinc-400 hover:text-white hover:bg-white/10 transition"
                  title="Copy Reference"
                >
                  {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <Badge variant="success" className="px-3 py-1 font-semibold text-xs">
              {booking.status || 'CONFIRMED'}
            </Badge>
          </div>

          {/* Ticket Body with Movie Info */}
          <div className="p-6 sm:p-8 space-y-6">
            <div className="flex gap-5 items-start">
              {moviePoster && (
                <img
                  src={moviePoster}
                  alt={movie?.title || 'Movie Poster'}
                  className="w-20 h-28 sm:w-24 sm:h-36 object-cover rounded-xl border border-white/10 shadow-lg shrink-0"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                  }}
                />
              )}
              <div className="space-y-2 flex-1 min-w-0">
                <h2 className="text-xl sm:text-2xl font-bold text-white truncate">
                  {movie?.title || 'Confirmed Movie Show'}
                </h2>
                <div className="flex flex-wrap gap-2 text-xs">
                  {show?.format && (
                    <span className="bg-red-600/20 text-red-400 border border-red-500/30 px-2 py-0.5 rounded font-semibold">
                      {show.format}
                    </span>
                  )}
                  {show?.language && (
                    <span className="bg-white/10 text-zinc-300 px-2 py-0.5 rounded font-medium">
                      {show.language}
                    </span>
                  )}
                  {movie?.certification && (
                    <span className="bg-white/10 text-zinc-300 px-2 py-0.5 rounded font-medium">
                      {movie.certification}
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-1.5 text-xs text-zinc-400 pt-1">
                  <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0" />
                  <span className="truncate">
                    {theatre?.name || 'Cinema Theatre'}, {theatre?.city || 'Vijayawada'}
                  </span>
                </div>
              </div>
            </div>

            {/* Perforated Divider */}
            <div className="relative border-t border-dashed border-white/15 my-6">
              <div className="absolute -left-10 -top-3 w-6 h-6 rounded-full bg-[#0A0D14] border-r border-white/10" />
              <div className="absolute -right-10 -top-3 w-6 h-6 rounded-full bg-[#0A0D14] border-l border-white/10" />
            </div>

            {/* Show & Seat Details Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs sm:text-sm">
              <div className="space-y-1">
                <span className="text-zinc-500 text-xs flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-zinc-400" />
                  Date
                </span>
                <p className="font-semibold text-white">{formattedDate}</p>
              </div>

              <div className="space-y-1">
                <span className="text-zinc-500 text-xs flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5 text-zinc-400" />
                  Time
                </span>
                <p className="font-semibold text-white">{formattedTime}</p>
              </div>

              <div className="space-y-1">
                <span className="text-zinc-500 text-xs flex items-center gap-1">
                  <Film className="w-3.5 h-3.5 text-zinc-400" />
                  Seats ({booking.bookingSeats?.length || 0})
                </span>
                <p className="font-semibold text-red-400">{seatLabels || 'Assigned'}</p>
              </div>

              <div className="space-y-1">
                <span className="text-zinc-500 text-xs">Total Paid</span>
                <p className="font-semibold text-white font-mono">
                  {booking.currency || 'INR'} {Number(booking.totalAmount).toLocaleString('en-IN')}
                </p>
              </div>
            </div>
          </div>

          {/* Action CTAs Footer */}
          <div className="bg-black/30 border-t border-white/10 p-6 flex flex-col sm:flex-row gap-3">
            <Button
              variant="primary"
              size="lg"
              onClick={() => navigate(`/bookings/${bookingId}/ticket`)}
              className="flex-1 gap-2 bg-red-600 hover:bg-red-500 shadow-lg shadow-red-900/30"
            >
              <TicketIcon className="w-5 h-5" />
              <span>View Digital Ticket Pass</span>
            </Button>
            <Button
              variant="outline"
              size="lg"
              onClick={() => navigate('/history')}
              className="border-white/20 hover:bg-white/10 text-white"
            >
              My Bookings
            </Button>
          </div>
        </div>

        {/* Helpful Tips Card */}
        <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-5 text-xs text-zinc-400 space-y-2">
          <div className="flex items-center gap-2 text-zinc-200 font-semibold text-sm">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>Admission Guidelines</span>
          </div>
          <ul className="list-disc list-inside space-y-1 text-zinc-400 leading-relaxed">
            <li>Please show your digital ticket QR code at the cinema entry gate.</li>
            <li>We recommend arriving at least 15 minutes before the showtime.</li>
            <li>Outside food and beverages are strictly prohibited inside the auditorium.</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default BookingConfirmationPage;
