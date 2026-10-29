import { useMemo } from 'react';
import { useNavigate } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { getMyBookings } from '../features/booking/api/bookingApi';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { LoadingState } from '../components/ui/LoadingState';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';
import {
  RotateCcw,
  ArrowRight,
  AlertCircle,
} from 'lucide-react';

export function RefundStatusPage() {
  const navigate = useNavigate();

  // Fetch bookings to find cancellations
  const {
    data: bookings = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['myBookings'],
    queryFn: () => getMyBookings(),
  });

  const cancelledBookings = useMemo(() => {
    if (!Array.isArray(bookings)) return [];
    return bookings.filter((b) => b.status === 'CANCELLED');
  }, [bookings]);

  return (
    <div className="min-h-[85vh] bg-[#0A0D14] text-white py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="border-b border-white/10 pb-6">
          <div className="flex items-center gap-2 mb-1">
            <RotateCcw className="w-6 h-6 text-red-500" />
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
              Refunds &amp; Reversals
            </h1>
          </div>
          <p className="text-sm text-zinc-400">
            Real-time status of refunds processed for cancelled cinema reservations.
          </p>
        </div>

        {/* Loading */}
        {isLoading && (
          <div className="py-16">
            <LoadingState message="Tracking your refund statuses..." />
          </div>
        )}

        {/* Error */}
        {isError && (
          <div className="py-12">
            <ErrorState
              title="Unable to Load Refunds"
              message={error?.message || 'Failed to check refund records.'}
              onRetry={refetch}
            />
          </div>
        )}

        {/* Empty */}
        {!isLoading && !isError && cancelledBookings.length === 0 && (
          <div className="py-16">
            <EmptyState
              icon={<RotateCcw className="w-12 h-12 text-zinc-500" />}
              title="No Pending or Completed Refunds"
              description="You do not have any cancelled bookings with initiated refunds."
              actionLabel="View Active Bookings"
              onAction={() => navigate('/history')}
            />
          </div>
        )}

        {/* Refunds List */}
        {!isLoading && !isError && cancelledBookings.length > 0 && (
          <div className="space-y-4">
            {cancelledBookings.map((b) => (
              <div
                key={b.bookingId}
                className="rounded-2xl border border-white/10 bg-[#121620] p-5 sm:p-6 transition hover:border-white/20 shadow-xl space-y-4"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/10 pb-4">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <Badge variant="success" className="text-[11px] font-bold">
                        REFUND INITIATED
                      </Badge>
                      <span className="text-xs font-mono text-zinc-400">
                        REF: {b.bookingId.substring(0, 10)}...
                      </span>
                    </div>
                    <h3 className="text-base sm:text-lg font-bold text-white">
                      Cancelled Cinema Reservation
                    </h3>
                  </div>

                  <div className="text-right">
                    <span className="text-xs text-zinc-500 block">Refund Amount</span>
                    <span className="font-mono font-bold text-lg text-emerald-400">
                      {b.currency || 'INR'} {Number(b.totalAmount).toLocaleString('en-IN')}
                    </span>
                  </div>
                </div>

                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-zinc-400">
                  <div className="space-y-1">
                    <p>
                      Cancellation processed. Refund routed back to original payment instrument.
                    </p>
                    {b.paymentId && (
                      <p className="font-mono text-zinc-500">Payment ID: {b.paymentId}</p>
                    )}
                  </div>

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => navigate(`/bookings/${b.bookingId}`)}
                    className="gap-1 border-white/20 text-xs self-start sm:self-auto"
                  >
                    <span>View Booking Audit</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Advisory */}
        <div className="rounded-2xl border border-white/10 bg-white/[0.02] p-4 text-xs text-zinc-400 space-y-1">
          <div className="flex items-center gap-2 text-zinc-300 font-semibold">
            <AlertCircle className="w-4 h-4 text-amber-400" />
            <span>Bank Settlement Timeline</span>
          </div>
          <p className="text-zinc-500 leading-relaxed">
            Reversals are processed immediately by BookNGo Payment Service. Depending on your bank or card
            issuer, credits reflect on your bank statement within standard banking hours.
          </p>
        </div>
      </div>
    </div>
  );
}

export default RefundStatusPage;
