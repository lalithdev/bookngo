import { useState, useEffect, useMemo } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router';
import { useQuery, useMutation } from '@tanstack/react-query';
import { getPaymentStatus, reconcilePayment } from '../features/payment/api/paymentApi';
import { getBookingById } from '../features/booking/api/bookingApi';
import { useToast } from '../context/useToast';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  Loader2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Clock,
  ShieldCheck,
  ArrowRight,
  RotateCcw,
} from 'lucide-react';

export function PaymentProcessingPage() {
  const { bookingId } = useParams();
  const [searchParams] = useSearchParams();
  const paymentId = searchParams.get('paymentId');
  const navigate = useNavigate();
  const toast = useToast();

  const [graceSecondsRemaining, setGraceSecondsRemaining] = useState(120); // 2-minute grace window
  const [pollCount, setPollCount] = useState(0);

  // 2-minute payment window countdown
  useEffect(() => {
    const timer = setInterval(() => {
      setGraceSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Poll payment status every 2 seconds until terminal
  const {
    data: paymentData,
    refetch: refetchStatus,
  } = useQuery({
    queryKey: ['paymentStatus', paymentId],
    queryFn: async () => {
      if (!paymentId) throw new Error('Payment ID is missing in query parameters');
      const res = await getPaymentStatus(paymentId);
      setPollCount((c) => c + 1);
      return res;
    },
    enabled: Boolean(paymentId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      // Stop polling on terminal states: SUCCESS, FAILURE, CANCELLED, TIMEOUT
      if (['SUCCESS', 'FAILURE', 'CANCELLED', 'TIMEOUT'].includes(status)) {
        return false;
      }
      return 2000; // Poll every 2 seconds
    },
    refetchIntervalInBackground: true,
  });

  // Fetch booking details for context
  const { data: bookingData } = useQuery({
    queryKey: ['booking', bookingId],
    queryFn: () => getBookingById(bookingId),
    enabled: Boolean(bookingId),
  });

  const status = paymentData?.status || 'PENDING';

  // Handle SUCCESS transition
  useEffect(() => {
    if (status === 'SUCCESS') {
      toast.success('Payment verified successfully! Generating your tickets...');
      const timeout = setTimeout(() => {
        navigate(`/confirmation/${bookingId}`, { replace: true });
      }, 1500);
      return () => clearTimeout(timeout);
    }
  }, [status, bookingId, navigate, toast]);

  // Reconcile mutation for UNKNOWN or TIMEOUT
  const reconcileMutation = useMutation({
    mutationFn: () => reconcilePayment(paymentId),
    onSuccess: (data) => {
      toast.info(`Status refreshed: ${data.status || 'Updated'}`);
      refetchStatus();
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to reconcile payment status.');
    },
  });

  const formattedTimer = useMemo(() => {
    const m = Math.floor(graceSecondsRemaining / 60);
    const s = graceSecondsRemaining % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }, [graceSecondsRemaining]);

  return (
    <div className="min-h-[85vh] bg-[#0A0D14] text-white py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-xl mx-auto">
        {/* Progress Step Header */}
        <div className="flex items-center justify-center gap-3 text-xs uppercase tracking-widest text-zinc-500 font-medium mb-8">
          <span>01 Seats</span>
          <span>&bull;</span>
          <span>02 Checkout</span>
          <span>&bull;</span>
          <span className="text-red-500 font-bold">03 Payment Processing</span>
          <span>&bull;</span>
          <span>04 Ticket</span>
        </div>

        {/* Processing Card */}
        <div className="rounded-2xl border border-white/10 bg-[#121620]/90 p-8 shadow-2xl backdrop-blur-xl relative overflow-hidden">
          {/* Subtle top glow bar */}
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-red-600 via-rose-500 to-amber-500" />

          {/* Grace Timer Banner */}
          {status !== 'SUCCESS' && status !== 'FAILURE' && status !== 'CANCELLED' && (
            <div className="mb-6 flex items-center justify-between rounded-xl bg-amber-500/10 border border-amber-500/20 px-4 py-2.5 text-xs text-amber-200">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-400 animate-pulse" />
                <span>Payment Confirmation Window:</span>
              </div>
              <span className="font-mono font-bold text-amber-300 text-sm">{formattedTimer}</span>
            </div>
          )}

          {/* STATE: PENDING / INITIATED */}
          {['INITIATED', 'PENDING'].includes(status) && (
            <div className="text-center py-6">
              <div className="relative mx-auto w-24 h-24 mb-6 flex items-center justify-center">
                {/* Radar pulse rings */}
                <div className="absolute inset-0 rounded-full border border-red-500/30 animate-ping opacity-75" />
                <div className="absolute inset-2 rounded-full border border-red-500/20 animate-pulse" />
                <div className="w-16 h-16 rounded-full bg-red-600/20 border border-red-500/40 flex items-center justify-center">
                  <Loader2 className="w-8 h-8 text-red-500 animate-spin" />
                </div>
              </div>

              <Badge variant="warning" className="mb-3 px-3 py-1 text-xs">
                AWAITING CONFIRMATION
              </Badge>

              <h2 className="text-2xl font-bold tracking-tight text-white mb-2">
                Verifying Payment with Provider
              </h2>
              <p className="text-sm text-zinc-400 max-w-md mx-auto mb-6">
                Please keep this window open while we communicate with the gateway. Your seats are
                protected while the transaction confirms.
              </p>

              {/* Polling heartbeat indicator */}
              <div className="inline-flex items-center gap-2 rounded-full bg-white/5 px-3 py-1 text-xs text-zinc-400">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                <span>Live Status Check #{pollCount}</span>
              </div>
            </div>
          )}

          {/* STATE: SUCCESS */}
          {status === 'SUCCESS' && (
            <div className="text-center py-6">
              <div className="mx-auto w-20 h-20 rounded-full bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center mb-6">
                <CheckCircle2 className="w-12 h-12 text-emerald-400 animate-bounce" />
              </div>

              <Badge variant="success" className="mb-3 px-3 py-1 text-xs">
                PAYMENT SUCCESSFUL
              </Badge>

              <h2 className="text-2xl font-bold tracking-tight text-white mb-2">
                Booking Confirmed!
              </h2>
              <p className="text-sm text-zinc-400 mb-6">
                Redirecting you to your digital ticket...
              </p>

              <Button
                variant="primary"
                onClick={() => navigate(`/confirmation/${bookingId}`)}
                className="w-full gap-2"
              >
                <span>View Confirmation Now</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </div>
          )}

          {/* STATE: FAILURE */}
          {status === 'FAILURE' && (
            <div className="text-center py-6">
              <div className="mx-auto w-20 h-20 rounded-full bg-rose-500/20 border border-rose-500/40 flex items-center justify-center mb-6">
                <XCircle className="w-12 h-12 text-rose-400" />
              </div>

              <Badge variant="danger" className="mb-3 px-3 py-1 text-xs">
                PAYMENT FAILED
              </Badge>

              <h2 className="text-2xl font-bold tracking-tight text-white mb-2">
                Transaction Declined
              </h2>
              <p className="text-sm text-zinc-400 max-w-md mx-auto mb-6">
                {paymentData?.failureReason ||
                  'The bank or payment provider declined the charge. No money was deducted from your account.'}
              </p>

              <div className="flex flex-col sm:flex-row gap-3">
                <Button
                  variant="primary"
                  onClick={() => navigate(`/checkout/${bookingId}`)}
                  className="flex-1 gap-2"
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>Try Payment Again</span>
                </Button>
                <Button
                  variant="outline"
                  onClick={() => navigate('/movies')}
                  className="flex-1"
                >
                  Browse Movies
                </Button>
              </div>
            </div>
          )}

          {/* STATE: CANCELLED */}
          {status === 'CANCELLED' && (
            <div className="text-center py-6">
              <div className="mx-auto w-20 h-20 rounded-full bg-zinc-500/20 border border-zinc-500/40 flex items-center justify-center mb-6">
                <XCircle className="w-12 h-12 text-zinc-400" />
              </div>

              <Badge variant="default" className="mb-3 px-3 py-1 text-xs">
                PAYMENT CANCELLED
              </Badge>

              <h2 className="text-2xl font-bold tracking-tight text-white mb-2">
                Payment Was Cancelled
              </h2>
              <p className="text-sm text-zinc-400 max-w-md mx-auto mb-6">
                The transaction was cancelled before completion. Your seat hold will be released automatically.
              </p>

              <Button
                variant="primary"
                onClick={() => navigate('/movies')}
                className="w-full"
              >
                Return to Discovery
              </Button>
            </div>
          )}

          {/* STATE: TIMEOUT / UNKNOWN */}
          {['TIMEOUT', 'UNKNOWN'].includes(status) && (
            <div className="text-center py-6">
              <div className="mx-auto w-20 h-20 rounded-full bg-amber-500/20 border border-amber-500/40 flex items-center justify-center mb-6">
                <AlertTriangle className="w-12 h-12 text-amber-400" />
              </div>

              <Badge variant="warning" className="mb-3 px-3 py-1 text-xs">
                STATUS PENDING RECONCILIATION
              </Badge>

              <h2 className="text-2xl font-bold tracking-tight text-white mb-2">
                Payment Verification Delayed
              </h2>
              <p className="text-sm text-zinc-400 max-w-md mx-auto mb-6">
                The gateway response took longer than expected. If your bank account was debited, your
                booking is safe and will update once reconciled. Please do not repay immediately.
              </p>

              <div className="flex flex-col gap-3">
                <Button
                  variant="primary"
                  onClick={() => reconcileMutation.mutate()}
                  isLoading={reconcileMutation.isPending}
                  className="w-full gap-2"
                >
                  <RefreshCw className="w-4 h-4" />
                  <span>Reconcile Payment Status</span>
                </Button>
                <div className="flex gap-3">
                  <Button
                    variant="outline"
                    onClick={() => navigate('/history')}
                    className="flex-1"
                  >
                    View My Bookings
                  </Button>
                  <Button
                    variant="ghost"
                    onClick={() => refetchStatus()}
                    className="flex-1"
                  >
                    Check Again
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* Order Details Metadata Footer */}
          <div className="mt-8 pt-6 border-t border-white/10 text-xs text-zinc-400 space-y-2">
            <div className="flex justify-between items-center">
              <span>Booking Reference:</span>
              <span className="font-mono text-white font-medium">{bookingId}</span>
            </div>
            {paymentId && (
              <div className="flex justify-between items-center">
                <span>Payment Reference:</span>
                <span className="font-mono text-zinc-300">{paymentId}</span>
              </div>
            )}
            {bookingData?.totalAmount && (
              <div className="flex justify-between items-center">
                <span>Total Amount:</span>
                <span className="text-white font-semibold">
                  {bookingData.currency || 'INR'} {Number(bookingData.totalAmount).toLocaleString('en-IN')}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Security Assurance */}
        <div className="mt-6 flex items-center justify-center gap-2 text-xs text-zinc-500">
          <ShieldCheck className="w-4 h-4 text-emerald-500" />
          <span>256-Bit Encrypted Cinema Payment Gateway Protocol</span>
        </div>
      </div>
    </div>
  );
}

export default PaymentProcessingPage;
