import { useState } from 'react';
import { useNavigate } from 'react-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/useAuth';
import { getCurrentUserProfile, updateCurrentUserProfile } from '../features/auth/api/authApi';
import { useToast } from '../context/useToast';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  User as UserIcon,
  Shield,
  Ticket,
  CreditCard,
  RotateCcw,
  LogOut,
  Save,
  Building,
  Sliders,
} from 'lucide-react';

export function ProfilePage() {
  const { user, isAuthenticated, isOperator, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();

  // Fetch fresh profile from GET /api/v1/users/me
  const { data: profile } = useQuery({
    queryKey: ['currentUserProfile'],
    queryFn: () => getCurrentUserProfile(),
    enabled: isAuthenticated,
  });

  const defaultFullName = profile?.fullName || user?.fullName || '';
  const defaultEmail = profile?.email || user?.email || '';

  const [formData, setFormData] = useState({ fullName: '', email: '' });
  const [isEditing, setIsEditing] = useState(false);

  const handleStartEditing = () => {
    setFormData({
      fullName: defaultFullName,
      email: defaultEmail,
    });
    setIsEditing(true);
  };

  // Update Profile Mutation
  const updateMutation = useMutation({
    mutationFn: (data) => updateCurrentUserProfile(data),
    onSuccess: (updated) => {
      toast.success('Profile details updated successfully');
      setIsEditing(false);
      queryClient.setQueryData(['currentUserProfile'], updated);
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to update profile');
    },
  });

  const handleSave = (e) => {
    e.preventDefault();
    updateMutation.mutate({
      fullName: (formData.fullName || defaultFullName).trim(),
      email: (formData.email || defaultEmail).trim() || null,
    });
  };

  if (!isAuthenticated) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4 bg-[#0A0D14]">
        <div className="w-16 h-16 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-zinc-400 mb-4">
          <UserIcon className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-bold text-white mb-2">Customer Sign In Required</h2>
        <p className="text-zinc-400 max-w-md mb-6 text-sm">
          Please sign in to your BookNGo customer account to view your reservations and cinema passes.
        </p>
        <Button variant="primary" onClick={() => navigate('/login')}>
          Sign In with Mobile OTP
        </Button>
      </div>
    );
  }

  const activeUser = profile || user;
  const displayName = activeUser?.fullName || 'BookNGo Customer';
  const displayPhone = activeUser?.phoneNumber || 'N/A';
  const displayEmail = activeUser?.email || 'No email attached';
  const roles = Array.isArray(activeUser?.roles) ? activeUser.roles : ['CUSTOMER'];

  return (
    <div className="min-h-[85vh] bg-[#0A0D14] text-white py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto space-y-8">
        {/* Header Breadcrumb */}
        <div className="border-b border-white/10 pb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <UserIcon className="w-6 h-6 text-red-500" />
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
                Account Settings
              </h1>
            </div>
            <p className="text-sm text-zinc-400">
              Personal information, security credentials, and quick account shortcuts.
            </p>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={logout}
            className="self-start sm:self-auto gap-2 border-white/20 text-rose-300 hover:text-rose-200 hover:border-rose-400/40 text-xs"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Sign Out</span>
          </Button>
        </div>

        {/* Profile Card */}
        <div className="rounded-3xl border border-white/10 bg-[#121620] p-6 sm:p-8 shadow-2xl space-y-8 relative overflow-hidden">
          {/* Subtle top glow */}
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-red-600 via-rose-600 to-amber-500" />

          {/* User Hero Banner */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6 pb-6 border-b border-white/10">
            <div className="flex items-center gap-4 sm:gap-5">
              <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-gradient-to-br from-red-600 to-rose-800 flex items-center justify-center text-white text-2xl sm:text-3xl font-black shadow-xl shadow-red-900/30 border border-white/20">
                {displayName
                  .split(' ')
                  .map((n) => n[0])
                  .join('')
                  .slice(0, 2)
                  .toUpperCase() || 'U'}
              </div>

              <div>
                <h2 className="text-xl sm:text-2xl font-black text-white tracking-tight">
                  {displayName}
                </h2>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xs sm:text-sm font-mono text-zinc-400">{displayPhone}</span>
                  <span className="w-1 h-1 rounded-full bg-zinc-600" />
                  <span className="text-xs text-zinc-400">{displayEmail}</span>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {roles.map((r) => (
                <Badge
                  key={r}
                  variant={r === 'ADMIN' ? 'danger' : r === 'THEATRE_OPERATOR' ? 'warning' : 'success'}
                  className="text-xs uppercase font-bold px-3 py-1"
                >
                  {r.replace('_', ' ')}
                </Badge>
              ))}
            </div>
          </div>

          {/* Special Portal Banners for Staff */}
          {(isOperator || isAdmin) && (
            <div className="rounded-2xl border border-amber-500/30 bg-amber-950/20 p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2 text-amber-300 font-bold text-sm">
                  <Shield className="w-4 h-4 text-amber-400" />
                  <span>Administrative Access Privileges Detected</span>
                </div>
                <p className="text-xs text-amber-200/80">
                  Your credentials grant access to specialized cinema operational portals.
                </p>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                {isOperator && (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => navigate('/operator')}
                    className="gap-1.5 bg-amber-600 hover:bg-amber-500 text-xs"
                  >
                    <Building className="w-3.5 h-3.5" />
                    <span>Operator Portal</span>
                  </Button>
                )}
                {isAdmin && (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => navigate('/admin')}
                    className="gap-1.5 bg-red-600 hover:bg-red-500 text-xs"
                  >
                    <Sliders className="w-3.5 h-3.5" />
                    <span>Admin Portal</span>
                  </Button>
                )}
              </div>
            </div>
          )}

          {/* Profile Form */}
          <form onSubmit={handleSave} className="space-y-6">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-semibold text-white">Profile Information</h3>
              {!isEditing ? (
                <button
                  type="button"
                  onClick={handleStartEditing}
                  className="text-xs text-red-400 hover:text-red-300 font-medium underline underline-offset-4 transition"
                >
                  Edit Information
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => setIsEditing(false)}
                  className="text-xs text-zinc-400 hover:text-white transition"
                >
                  Cancel
                </button>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                  Full Name
                </label>
                <input
                  type="text"
                  value={isEditing ? formData.fullName : defaultFullName}
                  onChange={(e) => setFormData((prev) => ({ ...prev, fullName: e.target.value }))}
                  disabled={!isEditing}
                  placeholder="Enter full name"
                  className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none transition disabled:opacity-60 focus:border-red-500/50 focus:bg-white/[0.08]"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                  Email Address
                </label>
                <input
                  type="email"
                  value={isEditing ? formData.email : defaultEmail}
                  onChange={(e) => setFormData((prev) => ({ ...prev, email: e.target.value }))}
                  disabled={!isEditing}
                  placeholder="name@example.com"
                  className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none transition disabled:opacity-60 focus:border-red-500/50 focus:bg-white/[0.08]"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                  Verified Phone
                </label>
                <div className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.02] text-zinc-400 text-sm flex items-center font-mono">
                  {displayPhone}
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                  Account Roles
                </label>
                <div className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.02] text-zinc-400 text-sm flex items-center">
                  {roles.join(', ')}
                </div>
              </div>
            </div>

            {isEditing && (
              <div className="flex justify-end pt-2">
                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  isLoading={updateMutation.isPending}
                  className="gap-2 bg-red-600 hover:bg-red-500"
                >
                  <Save className="w-4 h-4" />
                  <span>Save Changes</span>
                </Button>
              </div>
            )}
          </form>

          {/* Quick Shortcuts */}
          <div className="pt-6 border-t border-white/10 space-y-4">
            <h3 className="text-base font-semibold text-white">Cinema Shortcuts</h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <button
                type="button"
                onClick={() => navigate('/history')}
                className="p-4 rounded-2xl border border-white/10 bg-white/[0.02] hover:bg-white/[0.06] hover:border-white/20 transition text-left space-y-1 group"
              >
                <div className="flex items-center gap-2 text-white font-semibold text-sm group-hover:text-red-400">
                  <Ticket className="w-4 h-4 text-red-500" />
                  <span>My Bookings</span>
                </div>
                <p className="text-xs text-zinc-500">View upcoming &amp; past tickets</p>
              </button>

              <button
                type="button"
                onClick={() => navigate('/payments/history')}
                className="p-4 rounded-2xl border border-white/10 bg-white/[0.02] hover:bg-white/[0.06] hover:border-white/20 transition text-left space-y-1 group"
              >
                <div className="flex items-center gap-2 text-white font-semibold text-sm group-hover:text-red-400">
                  <CreditCard className="w-4 h-4 text-red-500" />
                  <span>Payments</span>
                </div>
                <p className="text-xs text-zinc-500">Statements &amp; invoices</p>
              </button>

              <button
                type="button"
                onClick={() => navigate('/refunds')}
                className="p-4 rounded-2xl border border-white/10 bg-white/[0.02] hover:bg-white/[0.06] hover:border-white/20 transition text-left space-y-1 group"
              >
                <div className="flex items-center gap-2 text-white font-semibold text-sm group-hover:text-red-400">
                  <RotateCcw className="w-4 h-4 text-red-500" />
                  <span>Refunds</span>
                </div>
                <p className="text-xs text-zinc-500">Track cancellation reversals</p>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ProfilePage;
