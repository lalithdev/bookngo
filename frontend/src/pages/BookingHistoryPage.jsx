import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { getMyBookings } from '../features/booking/api/bookingApi';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Tabs } from '../components/ui/Tabs';
import { LoadingState } from '../components/ui/LoadingState';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';
import {
  Ticket as TicketIcon,
  Film,
  ArrowRight,
  Sparkles,
} from 'lucide-react';

const TABS = [
  { id: 'UPCOMING', label: 'Upcoming' },
  { id: 'COMPLETED', label: 'Past / Completed' },
  { id: 'CANCELLED', label: 'Cancelled' },
  { id: 'ALL', label: 'All Bookings' },
];

export function BookingHistoryPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('UPCOMING');

  // 1. Fetch user bookings
  const {
    data: bookings = [],
    isLoading: isBookingsLoading,
    isError: isBookingsError,
    error: bookingsError,
    refetch: refetchBookings,
  } = useQuery({
    queryKey: ['myBookings'],
    queryFn: () => getMyBookings(),
  });

  // Filter bookings based on activeTab
  const filteredBookings = useMemo(() => {
    if (!Array.isArray(bookings)) return [];

    return bookings.filter((b) => {
      const isConfirmed = b.status === 'CONFIRMED' || b.status === 'TICKET_ISSUED';
      const isCancelled = b.status === 'CANCELLED' || b.status === 'EXPIRED';

      if (activeTab === 'UPCOMING') {
        return isConfirmed || b.status === 'PAYMENT_PENDING' || b.status === 'HELD';
      }
      if (activeTab === 'CANCELLED') {
        return isCancelled;
      }
      if (activeTab === 'COMPLETED') {
        // Any completed or past confirmed booking
        return b.status === 'COMPLETED';
      }
      return true; // 'ALL'
    });
  }, [bookings, activeTab]);

  return (
    <div className="min-h-[85vh] bg-[#0A0D14] text-white py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-white/10 pb-6">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <TicketIcon className="w-6 h-6 text-red-500" />
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
                My Cinema Bookings
              </h1>
            </div>
            <p className="text-sm text-zinc-400">
              Manage your cinema reservations, digital boarding passes, and cancellation requests.
            </p>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate('/movies')}
            className="self-start sm:self-auto gap-2 border-white/20 text-xs"
          >
            <Sparkles className="w-3.5 h-3.5 text-amber-400" />
            <span>Book New Tickets</span>
          </Button>
        </div>

        {/* Filter Tabs */}
        <div className="flex border-b border-white/10 overflow-x-auto pb-1">
          <Tabs
            tabs={TABS}
            activeTab={activeTab}
            onChange={setActiveTab}
          />
        </div>

        {/* Loading State */}
        {isBookingsLoading && (
          <div className="py-16">
            <LoadingState message="Retrieving your cinema bookings..." />
          </div>
        )}

        {/* Error State */}
        {isBookingsError && (
          <div className="py-12">
            <ErrorState
              title="Unable to Load Bookings"
              message={bookingsError?.message || 'Failed to connect to Booking Service.'}
              onRetry={refetchBookings}
            />
          </div>
        )}

        {/* Empty State */}
        {!isBookingsLoading && !isBookingsError && filteredBookings.length === 0 && (
          <div className="py-16">
            <EmptyState
              icon={<TicketIcon className="w-12 h-12 text-zinc-500" />}
              title={`No ${activeTab.toLowerCase()} bookings found`}
              description={
                activeTab === 'UPCOMING'
                  ? "You don't have any active cinema reservations scheduled."
                  : 'No booking records match this category.'
              }
              actionLabel="Explore Movies in Vijayawada"
              onAction={() => navigate('/movies')}
            />
          </div>
        )}

        {/* Bookings List */}
        {!isBookingsLoading && !isBookingsError && filteredBookings.length > 0 && (
          <div className="space-y-4">
            {filteredBookings.map((b) => {
              const seatLabels = (b.bookingSeats || [])
                .map((s) => s.seatLabelSnapshot)
                .filter(Boolean)
                .join(', ');

              const isConfirmed = b.status === 'CONFIRMED' || b.status === 'TICKET_ISSUED';
              const isCancelled = b.status === 'CANCELLED';

              const createdDate = b.createdAt
                ? new Date(b.createdAt).toLocaleDateString('en-IN', {
                    day: 'numeric',
                    month: 'short',
                    year: 'numeric',
                  })
                : 'Recent';

              return (
                <div
                  key={b.bookingId}
                  className="rounded-2xl border border-white/10 bg-[#121620] p-5 sm:p-6 transition hover:border-white/20 hover:bg-[#151a26] shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-5 relative overflow-hidden"
                >
                  {/* Left accent strip based on status */}
                  <div
                    className={`absolute left-0 top-0 bottom-0 w-1.5 ${
                      isConfirmed
                        ? 'bg-emerald-500'
                        : isCancelled
                        ? 'bg-rose-500'
                        : 'bg-amber-500'
                    }`}
                  />

                  {/* Booking Info */}
                  <div className="space-y-2 flex-1 min-w-0 pl-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge
                        variant={
                          isConfirmed ? 'success' : isCancelled ? 'danger' : 'warning'
                        }
                        className="text-[11px] font-semibold tracking-wider"
                      >
                        {b.status}
                      </Badge>
                      <span className="text-xs font-mono text-zinc-500">
                        REF: {b.bookingId.substring(0, 8)}...
                      </span>
                      <span className="text-xs text-zinc-500">• Booked on {createdDate}</span>
                    </div>

                    <h3 className="text-lg sm:text-xl font-bold text-white tracking-tight">
                      Cinema Experience Reservation
                    </h3>

                    {/* Meta details */}
                    <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-zinc-400">
                      <div className="flex items-center gap-1.5">
                        <Film className="w-3.5 h-3.5 text-red-500" />
                        <span>
                          Seats: <strong className="text-zinc-200">{seatLabels || `${b.bookingSeats?.length || 0} seats`}</strong>
                        </span>
                      </div>

                      <div className="flex items-center gap-1.5">
                        <span>Total Paid:</span>
                        <strong className="text-white font-mono">
                          {b.currency || 'INR'} {Number(b.totalAmount).toLocaleString('en-IN')}
                        </strong>
                      </div>
                    </div>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex items-center gap-2.5 w-full md:w-auto pt-2 md:pt-0 border-t md:border-t-0 border-white/10 shrink-0">
                    {isConfirmed && (
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={() => navigate(`/bookings/${b.bookingId}/ticket`)}
                        className="gap-1.5 bg-red-600 hover:bg-red-500 text-xs shadow-md shadow-red-900/20"
                      >
                        <TicketIcon className="w-3.5 h-3.5" />
                        <span>View Ticket</span>
                      </Button>
                    )}

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => navigate(`/bookings/${b.bookingId}`)}
                      className="gap-1.5 border-white/20 text-xs hover:bg-white/10 text-white"
                    >
                      <span>Details &amp; Policy</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export default BookingHistoryPage;
