import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { getTicketByBooking, getBookingById } from '../features/booking/api/bookingApi';
import { getShowById, getTheatres, getScreensByTheatre } from '../features/shows/api/showsAndTheatresApi';
import { getMovieById } from '../features/movies/api/moviesApi';
import { useToast } from '../context/useToast';
import { Button } from '../components/ui/Button';
import { LoadingState } from '../components/ui/LoadingState';
import { ErrorState } from '../components/ui/ErrorState';
import {
  Printer,
  Share2,
  Calendar,
  Clock,
  MapPin,
  Film,
  Copy,
  Check,
  ArrowLeft,
  ShieldCheck,
  AlertCircle,
} from 'lucide-react';

export function DigitalTicketPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [copied, setCopied] = useState(false);

  // 1. Fetch Ticket
  const {
    data: ticket,
    isLoading: isTicketLoading,
    isError: isTicketError,
    error: ticketError,
    refetch: refetchTicket,
  } = useQuery({
    queryKey: ['ticket', bookingId],
    queryFn: () => getTicketByBooking(bookingId),
    enabled: Boolean(bookingId),
  });

  // 2. Fetch Booking (for showId, seats snapshot, totalAmount)
  const { data: booking } = useQuery({
    queryKey: ['booking', bookingId],
    queryFn: () => getBookingById(bookingId),
    enabled: Boolean(bookingId),
  });

  // 3. Fetch Show
  const showId = booking?.showId || ticket?.historicalSnapshot?.showId;
  const { data: show } = useQuery({
    queryKey: ['show', showId],
    queryFn: () => getShowById(showId),
    enabled: Boolean(showId),
  });

  // 4. Fetch Movie
  const { data: movie } = useQuery({
    queryKey: ['movie', show?.movieId],
    queryFn: () => getMovieById(show.movieId),
    enabled: Boolean(show?.movieId),
  });

  // 5. Fetch Theatre
  const { data: theatres = [] } = useQuery({
    queryKey: ['theatres'],
    queryFn: () => getTheatres(),
  });
  const theatre = theatres.find((t) => t.theatreId === show?.theatreId);

  // 6. Fetch Screens for this theatre
  const { data: screens = [] } = useQuery({
    queryKey: ['theatreScreens', show?.theatreId],
    queryFn: () => getScreensByTheatre(show.theatreId),
    enabled: Boolean(show?.theatreId),
  });
  const screen = screens.find((s) => s.screenId === show?.screenId);

  const handlePrint = () => {
    window.print();
  };

  const handleCopyCode = () => {
    if (!ticket?.ticketCode) return;
    navigator.clipboard.writeText(ticket.ticketCode);
    setCopied(true);
    toast.success('Ticket code copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: `BookNGo Ticket: ${movie?.title || 'Movie'}`,
        text: `My movie ticket for ${movie?.title || 'Cinema'} on ${formattedDate} at ${formattedTime}. Ticket Code: ${ticket?.ticketCode}`,
        url: window.location.href,
      }).catch(() => {});
    } else {
      navigator.clipboard.writeText(window.location.href);
      toast.success('Ticket link copied to clipboard');
    }
  };

  if (isTicketLoading) {
    return (
      <div className="min-h-[85vh] flex items-center justify-center bg-[#0A0D14]">
        <LoadingState message="Generating your cinema digital pass..." />
      </div>
    );
  }

  if (isTicketError || !ticket) {
    return (
      <div className="min-h-[85vh] flex items-center justify-center bg-[#0A0D14] p-4">
        <ErrorState
          title="Digital Ticket Unavailable"
          message={
            ticketError?.message ||
            'Ticket is only generated for confirmed bookings. If your payment is still processing, please check back shortly.'
          }
          onRetry={refetchTicket}
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
    : 'Scheduled Date';

  const formattedTime = showDateTime
    ? showDateTime.toLocaleTimeString('en-IN', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
      })
    : 'Scheduled Time';

  const seats = booking?.bookingSeats || ticket?.historicalSnapshot?.seats || [];
  const seatLabels = seats.map((s) => s.seatLabelSnapshot || s.seatLabel).filter(Boolean);

  const moviePoster =
    movie?.posterUrl || (movie?.title ? `/posters/${encodeURIComponent(movie.title)}.jpg` : null);

  const isCancelled = ticket.status === 'CANCELLED';

  return (
    <div className="min-h-[90vh] bg-[#0A0D14] text-white py-8 sm:py-12 px-4 sm:px-6 print:p-0 print:bg-white print:text-black">
      <div className="max-w-md sm:max-w-lg mx-auto space-y-6">
        {/* Navigation & Action Bar (Hidden on print) */}
        <div className="flex items-center justify-between print:hidden">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-white transition"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back</span>
          </button>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handlePrint}
              className="gap-1.5 border-white/20 text-xs"
            >
              <Printer className="w-3.5 h-3.5" />
              <span>Print Ticket</span>
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleShare}
              className="gap-1.5 border-white/20 text-xs"
            >
              <Share2 className="w-3.5 h-3.5" />
              <span>Share</span>
            </Button>
          </div>
        </div>

        {/* Cancelled Banner */}
        {isCancelled && (
          <div className="rounded-xl border border-rose-500/30 bg-rose-950/40 p-4 text-sm text-rose-200 flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
            <span>This ticket has been cancelled. Admission is no longer permitted.</span>
          </div>
        )}

        {/* DIGITAL CINEMA BOARDING PASS */}
        <div className="rounded-3xl border border-white/15 bg-[#121620] overflow-hidden shadow-[0_20px_60px_rgba(0,0,0,0.8)] relative print:border-black print:bg-white print:shadow-none">
          {/* Top Cinema Header */}
          <div className="bg-gradient-to-r from-red-600 via-red-500 to-rose-600 p-4 sm:p-5 text-white flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Film className="w-5 h-5" />
              <span className="font-extrabold tracking-wider text-base uppercase">BookNGo Cinema Pass</span>
            </div>
            <span className="text-[11px] font-mono tracking-widest bg-black/30 px-2.5 py-1 rounded-full uppercase">
              Admit {seats.length || 1}
            </span>
          </div>

          {/* Upper Section: Movie & Theatre Info */}
          <div className="p-6 sm:p-7 space-y-6">
            <div className="flex gap-4 sm:gap-5 items-start">
              {moviePoster && (
                <img
                  src={moviePoster}
                  alt={movie?.title || 'Movie'}
                  className="w-20 h-28 object-cover rounded-xl border border-white/10 shadow-md shrink-0 print:border-black"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                  }}
                />
              )}
              <div className="space-y-1.5 flex-1 min-w-0">
                <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-white print:text-black leading-tight">
                  {movie?.title || 'Selected Movie'}
                </h2>
                <div className="flex flex-wrap gap-1.5 text-[11px]">
                  {show?.format && (
                    <span className="bg-red-500/20 text-red-400 border border-red-500/30 px-2 py-0.5 rounded font-semibold print:border-black print:text-black">
                      {show.format}
                    </span>
                  )}
                  {show?.language && (
                    <span className="bg-white/10 text-zinc-300 px-2 py-0.5 rounded print:text-black">
                      {show.language}
                    </span>
                  )}
                  {movie?.certification && (
                    <span className="bg-white/10 text-zinc-300 px-2 py-0.5 rounded print:text-black">
                      {movie.certification}
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-1.5 text-xs text-zinc-400 pt-1 print:text-zinc-700">
                  <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0" />
                  <span className="truncate">
                    {theatre?.name || 'Cinema Multiplex'}, {theatre?.city || 'Vijayawada'}
                  </span>
                </div>
              </div>
            </div>

            {/* Screen / Auditorium Info */}
            <div className="rounded-xl bg-white/[0.04] p-3.5 border border-white/10 flex items-center justify-between text-xs sm:text-sm print:bg-zinc-100 print:border-black">
              <div>
                <span className="text-zinc-500 text-[11px] block">Auditorium / Screen</span>
                <span className="font-bold text-white print:text-black">
                  {screen?.screenName || 'Main Screen'}
                </span>
              </div>
              <div className="text-right">
                <span className="text-zinc-500 text-[11px] block">Sound &amp; Projection</span>
                <span className="font-medium text-zinc-300 print:text-black">
                  {show?.format || 'Dolby Atmos'}
                </span>
              </div>
            </div>

            {/* Showtime & Date */}
            <div className="grid grid-cols-2 gap-4">
              <div className="rounded-xl border border-white/10 bg-white/[0.02] p-3 print:border-black">
                <div className="flex items-center gap-1.5 text-zinc-500 text-xs mb-1">
                  <Calendar className="w-3.5 h-3.5 text-red-400" />
                  <span>Date</span>
                </div>
                <p className="font-semibold text-white print:text-black text-sm">{formattedDate}</p>
              </div>

              <div className="rounded-xl border border-white/10 bg-white/[0.02] p-3 print:border-black">
                <div className="flex items-center gap-1.5 text-zinc-500 text-xs mb-1">
                  <Clock className="w-3.5 h-3.5 text-red-400" />
                  <span>Showtime</span>
                </div>
                <p className="font-semibold text-white print:text-black text-sm">{formattedTime}</p>
              </div>
            </div>

            {/* Reserved Seats List */}
            <div>
              <span className="text-zinc-500 text-xs block mb-2">Reserved Seats</span>
              <div className="flex flex-wrap gap-2">
                {seatLabels.length > 0 ? (
                  seatLabels.map((lbl, idx) => (
                    <span
                      key={idx}
                      className="px-3 py-1.5 rounded-lg bg-red-600/20 border border-red-500/40 text-red-400 font-bold font-mono text-sm print:border-black print:text-black"
                    >
                      {lbl}
                    </span>
                  ))
                ) : (
                  <span className="text-xs text-zinc-400">Assigned at entry</span>
                )}
              </div>
            </div>
          </div>

          {/* PERFORATED DIVIDER NOTCHES */}
          <div className="relative border-t-2 border-dashed border-white/15 my-1 print:border-black">
            <div className="absolute -left-4 -top-3 w-7 h-7 rounded-full bg-[#0A0D14] border-r border-white/15 print:hidden" />
            <div className="absolute -right-4 -top-3 w-7 h-7 rounded-full bg-[#0A0D14] border-l border-white/15 print:hidden" />
          </div>

          {/* Lower Stub: Verification QR & Ticket Code */}
          <div className="p-6 sm:p-7 bg-black/40 space-y-5 text-center print:bg-white">
            <div className="flex flex-col items-center justify-center">
              {/* High Contrast QR Code Visual representation */}
              <div className="p-3 bg-white rounded-2xl shadow-xl border border-white/20 mb-3 inline-block">
                <svg
                  className="w-36 h-36 sm:w-40 sm:h-40"
                  viewBox="0 0 100 100"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  {/* Position detection patterns */}
                  <rect x="10" y="10" width="24" height="24" rx="3" fill="black" />
                  <rect x="14" y="14" width="16" height="16" rx="2" fill="white" />
                  <rect x="18" y="18" width="8" height="8" rx="1" fill="black" />

                  <rect x="66" y="10" width="24" height="24" rx="3" fill="black" />
                  <rect x="70" y="14" width="16" height="16" rx="2" fill="white" />
                  <rect x="74" y="18" width="8" height="8" rx="1" fill="black" />

                  <rect x="10" y="66" width="24" height="24" rx="3" fill="black" />
                  <rect x="14" y="70" width="16" height="16" rx="2" fill="white" />
                  <rect x="18" y="74" width="8" height="8" rx="1" fill="black" />

                  {/* Dynamic simulated data grid */}
                  <rect x="42" y="12" width="4" height="4" fill="black" />
                  <rect x="50" y="16" width="6" height="4" fill="black" />
                  <rect x="42" y="24" width="8" height="4" fill="black" />
                  <rect x="14" y="44" width="6" height="6" fill="black" />
                  <rect x="24" y="48" width="8" height="4" fill="black" />
                  <rect x="40" y="40" width="20" height="20" rx="2" fill="black" />
                  <rect x="44" y="44" width="12" height="12" rx="1" fill="white" />
                  <rect x="48" y="48" width="4" height="4" fill="black" />
                  <rect x="68" y="44" width="6" height="4" fill="black" />
                  <rect x="78" y="40" width="4" height="8" fill="black" />
                  <rect x="42" y="68" width="8" height="4" fill="black" />
                  <rect x="54" y="72" width="4" height="8" fill="black" />
                  <rect x="68" y="68" width="14" height="6" fill="black" />
                  <rect x="72" y="80" width="12" height="6" fill="black" />
                </svg>
              </div>

              <span className="text-[11px] uppercase tracking-wider text-zinc-400 print:text-zinc-700">
                Scan at Gate for Entry
              </span>
            </div>

            {/* Ticket Code with Copy */}
            <div className="space-y-1">
              <span className="text-[11px] uppercase tracking-widest text-zinc-500">Ticket Pass Code</span>
              <div className="flex items-center justify-center gap-2">
                <span className="font-mono text-lg sm:text-xl font-extrabold text-white tracking-widest print:text-black">
                  {ticket.ticketCode}
                </span>
                <button
                  type="button"
                  onClick={handleCopyCode}
                  className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-white/10 transition print:hidden"
                  title="Copy Ticket Code"
                >
                  {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Barcode Strip Graphic */}
            <div className="pt-2">
              <div className="h-8 w-full max-w-[280px] mx-auto flex items-center justify-between opacity-70 print:opacity-100">
                {[3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2, 1, 3, 4, 2, 1, 3, 2].map(
                  (w, i) => (
                    <div
                      key={i}
                      className="bg-white print:bg-black h-full rounded-sm"
                      style={{ width: `${w * 2}px` }}
                    />
                  )
                )}
              </div>
              <span className="font-mono text-[10px] text-zinc-500 block mt-1 tracking-widest">
                REF: {bookingId}
              </span>
            </div>
          </div>
        </div>

        {/* Security & Entry Notice */}
        <div className="rounded-2xl border border-white/10 bg-white/[0.02] p-4 text-xs text-zinc-400 space-y-1 text-center print:hidden">
          <div className="flex items-center justify-center gap-1.5 text-zinc-300 font-medium">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>Digital Pass Active &amp; Valid for Entry</span>
          </div>
          <p className="text-zinc-500">
            Please keep brightness up when scanning at cinema barrier. Non-transferable admission.
          </p>
        </div>

        {/* Action Link back */}
        <div className="text-center print:hidden">
          <Link
            to="/history"
            className="text-xs text-zinc-400 hover:text-white underline underline-offset-4 transition"
          >
            Go to My Bookings History
          </Link>
        </div>
      </div>
    </div>
  );
}

export default DigitalTicketPage;
