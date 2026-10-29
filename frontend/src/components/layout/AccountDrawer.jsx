import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router';
import {
  User,
  Ticket,
  Clock,
  CheckCircle2,
  XCircle,
  CreditCard,
  RotateCcw,
  LogOut,
  X,
  ChevronRight,
} from 'lucide-react';
import { useAuth } from '../../context/useAuth';
import { IconButton } from '../ui/IconButton';

export function AccountDrawer({ isOpen, onClose }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Handle escape key to close
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Lock body scroll when drawer is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSignOut = () => {
    logout();
    onClose();
    navigate('/');
  };

  const displayName = user?.fullName || 'BookNGo Customer';
  const displayPhone = user?.phoneNumber || '';
  const initials = displayName
    ? displayName
        .split(' ')
        .map((n) => n[0])
        .join('')
        .slice(0, 2)
        .toUpperCase()
    : 'U';

  return (
    <div
      className="fixed inset-0 z-50 flex justify-end"
      role="dialog"
      aria-modal="true"
      aria-labelledby="account-drawer-title"
    >
      {/* Dimmed backdrop */}
      <div
        className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Slide-over panel */}
      <div className="relative w-full max-w-md bg-slate-950 border-l border-slate-800 shadow-2xl flex flex-col h-full z-10 overflow-y-auto">
        {/* Drawer Header */}
        <div className="p-6 border-b border-slate-800/80 flex items-center justify-between bg-slate-900/60">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-red-600 to-red-800 flex items-center justify-center text-white font-black text-lg shadow-lg shadow-red-600/30 border border-red-500/30">
              {initials}
            </div>
            <div>
              <h2
                id="account-drawer-title"
                className="text-base font-bold text-white tracking-tight leading-tight"
              >
                {displayName}
              </h2>
              {displayPhone && (
                <p className="text-xs text-slate-400 font-mono mt-0.5">
                  {displayPhone}
                </p>
              )}
            </div>
          </div>

          <IconButton
            icon={<X className="w-4 h-4" />}
            variant="ghost"
            size="sm"
            onClick={onClose}
            ariaLabel="Close account menu"
          />
        </div>

        {/* Drawer Body Navigation */}
        <div className="p-6 space-y-6 flex-1">
          {/* Section: Account */}
          <div className="space-y-1.5">
            <div className="text-[10px] font-black uppercase tracking-wider text-slate-500 px-3">
              Account
            </div>
            <Link
              to="/profile"
              onClick={onClose}
              className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-200 hover:text-white hover:bg-slate-900/90 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <span className="p-1.5 rounded-lg bg-red-600/10 border border-red-500/20 text-red-500">
                  <User className="w-4 h-4" />
                </span>
                <span>Profile Details</span>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-slate-300 transition-transform group-hover:translate-x-0.5" />
            </Link>
          </div>

          {/* Section: Bookings */}
          <div className="space-y-1.5">
            <div className="text-[10px] font-black uppercase tracking-wider text-slate-500 px-3">
              Bookings &amp; Tickets
            </div>

            <Link
              to="/history?tab=upcoming"
              onClick={onClose}
              className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-200 hover:text-white hover:bg-slate-900/90 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <span className="p-1.5 rounded-lg bg-emerald-600/10 border border-emerald-500/20 text-emerald-400">
                  <Clock className="w-4 h-4" />
                </span>
                <span>Upcoming Shows</span>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-slate-300 transition-transform group-hover:translate-x-0.5" />
            </Link>

            <Link
              to="/history?tab=past"
              onClick={onClose}
              className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-200 hover:text-white hover:bg-slate-900/90 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <span className="p-1.5 rounded-lg bg-slate-800 border border-slate-700/80 text-slate-300">
                  <CheckCircle2 className="w-4 h-4" />
                </span>
                <span>Completed / Past</span>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-slate-300 transition-transform group-hover:translate-x-0.5" />
            </Link>

            <Link
              to="/history?tab=cancelled"
              onClick={onClose}
              className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-200 hover:text-white hover:bg-slate-900/90 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <span className="p-1.5 rounded-lg bg-rose-600/10 border border-rose-500/20 text-rose-400">
                  <XCircle className="w-4 h-4" />
                </span>
                <span>Cancelled Bookings</span>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-slate-300 transition-transform group-hover:translate-x-0.5" />
            </Link>

            <Link
              to="/tickets"
              onClick={onClose}
              className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-200 hover:text-white hover:bg-slate-900/90 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <span className="p-1.5 rounded-lg bg-cyan-600/10 border border-cyan-500/20 text-cyan-400">
                  <Ticket className="w-4 h-4" />
                </span>
                <span>Digital Passes</span>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-slate-300 transition-transform group-hover:translate-x-0.5" />
            </Link>
          </div>

          {/* Section: Payments & Refunds */}
          <div className="space-y-1.5">
            <div className="text-[10px] font-black uppercase tracking-wider text-slate-500 px-3">
              Payments &amp; Refunds
            </div>

            <Link
              to="/payments/history"
              onClick={onClose}
              className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-200 hover:text-white hover:bg-slate-900/90 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <span className="p-1.5 rounded-lg bg-slate-800 border border-slate-700/80 text-slate-300">
                  <CreditCard className="w-4 h-4" />
                </span>
                <span>Payment History</span>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-slate-300 transition-transform group-hover:translate-x-0.5" />
            </Link>

            <Link
              to="/refunds"
              onClick={onClose}
              className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-200 hover:text-white hover:bg-slate-900/90 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <span className="p-1.5 rounded-lg bg-amber-600/10 border border-amber-500/20 text-amber-400">
                  <RotateCcw className="w-4 h-4" />
                </span>
                <span>Refund &amp; Cancellation Status</span>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-500 group-hover:text-slate-300 transition-transform group-hover:translate-x-0.5" />
            </Link>
          </div>
        </div>

        {/* Drawer Footer */}
        <div className="p-6 border-t border-slate-800/80 bg-slate-900/40">
          <button
            onClick={handleSignOut}
            className="w-full py-2.5 px-4 rounded-xl border border-red-500/30 text-red-400 hover:text-white hover:bg-red-600/20 hover:border-red-500/50 transition-colors text-sm font-semibold flex items-center justify-center gap-2 cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>
        </div>
      </div>
    </div>
  );
}

export default AccountDrawer;
