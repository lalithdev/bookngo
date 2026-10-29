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
  CreditCard,
  CheckCircle2,
  Clock,
  ArrowRight,
  ShieldCheck,
  Receipt,
} from 'lucide-react';

export function PaymentHistoryPage() {
  const navigate = useNavigate();

  // Fetch bookings to derive customer payment transactions
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

  const transactions = useMemo(() => {
    if (!Array.isArray(bookings)) return [];
    return bookings
      .filter((b) => b.totalAmount && Number(b.totalAmount) > 0)
      .map((b) => {
        const isPaid = b.status === 'CONFIRMED' || b.status === 'TICKET_ISSUED';
        const isCancelled = b.status === 'CANCELLED';

        return {
          id: b.paymentId || b.bookingId,
          bookingId: b.bookingId,
          amount: b.totalAmount,
          currency: b.currency || 'INR',
          status: isPaid ? 'SUCCESS' : isCancelled ? 'REFUNDED' : b.status,
          date: b.createdAt
            ? new Date(b.createdAt).toLocaleDateString('en-IN', {
                day: 'numeric',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })
            : 'Recent',
          seatsCount: b.bookingSeats?.length || 1,
        };
      });
  }, [bookings]);

  return (
    <div className="min-h-[85vh] bg-[#0A0D14] text-white py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="border-b border-white/10 pb-6 flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <CreditCard className="w-6 h-6 text-red-500" />
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
                Payment History
              </h1>
            </div>
            <p className="text-sm text-zinc-400">
              Audit trail and statements for all cinema bookings and digital transactions.
            </p>
          </div>
        </div>

        {/* Loading */}
        {isLoading && (
          <div className="py-16">
            <LoadingState message="Loading your transaction history..." />
          </div>
        )}

        {/* Error */}
        {isError && (
          <div className="py-12">
            <ErrorState
              title="Unable to Load Payments"
              message={error?.message || 'Failed to retrieve transaction records.'}
              onRetry={refetch}
            />
          </div>
        )}

        {/* Empty */}
        {!isLoading && !isError && transactions.length === 0 && (
          <div className="py-16">
            <EmptyState
              icon={<Receipt className="w-12 h-12 text-zinc-500" />}
              title="No Payment History Found"
              description="You have not made any ticket payments or reservations yet."
              actionLabel="Browse Movies Now"
              onAction={() => navigate('/movies')}
            />
          </div>
        )}

        {/* Transactions Table / List */}
        {!isLoading && !isError && transactions.length > 0 && (
          <div className="space-y-3">
            {transactions.map((tx) => {
              const isSuccess = tx.status === 'SUCCESS';
              const isRefunded = tx.status === 'REFUNDED';

              return (
                <div
                  key={tx.id}
                  className="rounded-2xl border border-white/10 bg-[#121620] p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition hover:border-white/20"
                >
                  <div className="flex items-start gap-3.5">
                    <div
                      className={`p-2.5 rounded-xl border mt-0.5 ${
                        isSuccess
                          ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                          : isRefunded
                          ? 'bg-purple-500/10 border-purple-500/30 text-purple-300'
                          : 'bg-zinc-800 border-zinc-700 text-zinc-400'
                      }`}
                    >
                      {isSuccess ? (
                        <CheckCircle2 className="w-5 h-5" />
                      ) : isRefunded ? (
                        <Receipt className="w-5 h-5" />
                      ) : (
                        <Clock className="w-5 h-5" />
                      )}
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-white text-sm sm:text-base">
                          Movie Ticket Reservation
                        </span>
                        <Badge
                          variant={isSuccess ? 'success' : isRefunded ? 'default' : 'warning'}
                          className="text-[10px] uppercase font-bold"
                        >
                          {tx.status}
                        </Badge>
                      </div>

                      <div className="text-xs text-zinc-400 flex flex-wrap items-center gap-x-3 gap-y-1">
                        <span>{tx.date}</span>
                        <span>&bull;</span>
                        <span className="font-mono text-zinc-500">Ref: {tx.bookingId.substring(0, 13)}...</span>
                        <span>&bull;</span>
                        <span>{tx.seatsCount} Seats</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between sm:justify-end gap-4 border-t sm:border-t-0 pt-2 sm:pt-0 border-white/10">
                    <span className="font-mono font-bold text-base sm:text-lg text-white">
                      {tx.currency} {Number(tx.amount).toLocaleString('en-IN')}
                    </span>

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => navigate(`/bookings/${tx.bookingId}`)}
                      className="gap-1 border-white/20 text-xs"
                    >
                      <span>Receipt</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Security Info */}
        <div className="pt-4 flex items-center justify-center gap-2 text-xs text-zinc-500">
          <ShieldCheck className="w-4 h-4 text-emerald-500" />
          <span>All payment transactions are verified and signed by Payment Service</span>
        </div>
      </div>
    </div>
  );
}

export default PaymentHistoryPage;
