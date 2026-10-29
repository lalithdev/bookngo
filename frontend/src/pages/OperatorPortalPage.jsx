import { useState, useMemo } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  getTheatres,
  getScreensByTheatre,
  getPhysicalSeatsByScreen,
  getShows,
  getShowPricing,
  updateShowPricing,
  getCancellationPolicyByShow,
  createShow,
  createScreen,
} from '../features/shows/api/showsAndTheatresApi';
import { getMovies } from '../features/movies/api/moviesApi';
import { fetchSeatInventory } from '../features/seats/api/seatsApi';
import { useToast } from '../context/useToast';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Tabs } from '../components/ui/Tabs';
import { LoadingState } from '../components/ui/LoadingState';
import {
  Building2,
  Tv,
  Film,
  DollarSign,
  Plus,
  Edit,
  Shield,
  RefreshCw,
  Layers,
  Percent,
} from 'lucide-react';

const OPERATOR_TABS = [
  { id: 'THEATRES_SCREENS', label: 'Theatres & Screens' },
  { id: 'PHYSICAL_SEATS', label: 'Screen Seat Layout' },
  { id: 'SHOW_SCHEDULE', label: 'Show Scheduling' },
  { id: 'PRICING_POLICY', label: 'Pricing & Policy' },
  { id: 'LIVE_OCCUPANCY', label: 'Live Occupancy' },
];

export function OperatorPortalPage() {
  const toast = useToast();

  const [activeTab, setActiveTab] = useState('THEATRES_SCREENS');
  const [selectedTheatreId, setSelectedTheatreId] = useState('');
  const [selectedScreenId, setSelectedScreenId] = useState('');
  const [selectedShowId, setSelectedShowId] = useState('');

  // Modals
  const [newScreenModalOpen, setNewScreenModalOpen] = useState(false);
  const [newScreenName, setNewScreenName] = useState('');
  const [newScreenCapacity, setNewScreenCapacity] = useState('100');

  const [newShowModalOpen, setNewShowModalOpen] = useState(false);
  const [newShowMovieId, setNewShowMovieId] = useState('');
  const [newShowDate, setNewShowDate] = useState(new Date().toISOString().split('T')[0]);
  const [newShowStartTime, setNewShowStartTime] = useState('14:30');
  const [newShowEndTime, setNewShowEndTime] = useState('17:15');
  const [newShowFormat, setNewShowFormat] = useState('2D');
  const [newShowLanguage, setNewShowLanguage] = useState('Telugu');

  const [pricingEditOpen, setPricingEditOpen] = useState(false);
  const [vipPrice, setVipPrice] = useState('350');
  const [premiumPrice, setPremiumPrice] = useState('250');
  const [standardPrice, setStandardPrice] = useState('180');

  // 1. Fetch Theatres
  const { data: theatres = [] } = useQuery({
    queryKey: ['theatres'],
    queryFn: () => getTheatres(),
    onSuccess: (data) => {
      if (data.length > 0 && !selectedTheatreId) {
        setSelectedTheatreId(data[0].theatreId);
      }
    },
  });

  // Set default selected theatre
  const activeTheatre = useMemo(() => {
    return theatres.find((t) => t.theatreId === selectedTheatreId) || theatres[0] || null;
  }, [theatres, selectedTheatreId]);

  const effectiveTheatreId = activeTheatre?.theatreId || '';

  // 2. Fetch Screens for active theatre
  const {
    data: screens = [],
    isLoading: isScreensLoading,
    refetch: refetchScreens,
  } = useQuery({
    queryKey: ['theatreScreens', effectiveTheatreId],
    queryFn: () => getScreensByTheatre(effectiveTheatreId),
    enabled: Boolean(effectiveTheatreId),
    onSuccess: (data) => {
      if (data.length > 0 && !selectedScreenId) {
        setSelectedScreenId(data[0].screenId);
      }
    },
  });

  const activeScreen = useMemo(() => {
    return screens.find((s) => s.screenId === selectedScreenId) || screens[0] || null;
  }, [screens, selectedScreenId]);

  const effectiveScreenId = activeScreen?.screenId || '';

  // 3. Fetch Physical Seats for active screen
  const { data: physicalSeats = [], isLoading: isSeatsLoading } = useQuery({
    queryKey: ['screenSeats', effectiveScreenId],
    queryFn: () => getPhysicalSeatsByScreen(effectiveScreenId),
    enabled: Boolean(effectiveScreenId),
  });

  // 4. Fetch Shows for active theatre
  const {
    data: shows = [],
    isLoading: isShowsLoading,
    refetch: refetchShows,
  } = useQuery({
    queryKey: ['theatreShows', effectiveTheatreId],
    queryFn: () => getShows({ theatreId: effectiveTheatreId }),
    enabled: Boolean(effectiveTheatreId),
    onSuccess: (data) => {
      if (data.length > 0 && !selectedShowId) {
        setSelectedShowId(data[0].showId);
      }
    },
  });

  const activeShow = useMemo(() => {
    return shows.find((s) => s.showId === selectedShowId) || shows[0] || null;
  }, [shows, selectedShowId]);

  const effectiveShowId = activeShow?.showId || '';

  // 5. Fetch Movies catalog for show creator
  const { data: movies = [] } = useQuery({
    queryKey: ['movies'],
    queryFn: () => getMovies(),
  });

  // 6. Fetch Show Pricing for active show
  const { data: showPricing = [], refetch: refetchPricing } = useQuery({
    queryKey: ['showPricing', effectiveShowId],
    queryFn: () => getShowPricing(effectiveShowId),
    enabled: Boolean(effectiveShowId),
  });

  // 7. Fetch Cancellation Policy for active show
  const { data: cancellationPolicy } = useQuery({
    queryKey: ['cancellationPolicy', effectiveShowId],
    queryFn: () => getCancellationPolicyByShow(effectiveShowId),
    enabled: Boolean(effectiveShowId),
  });

  // 8. Fetch Live Seat Inventory for active show
  const { data: seatInventory = [], refetch: refetchInventory } = useQuery({
    queryKey: ['showInventory', effectiveShowId],
    queryFn: () => fetchSeatInventory(effectiveShowId),
    enabled: Boolean(effectiveShowId && activeTab === 'LIVE_OCCUPANCY'),
    refetchInterval: activeTab === 'LIVE_OCCUPANCY' ? 5000 : false,
  });

  // Create Screen Mutation
  const createScreenMutation = useMutation({
    mutationFn: () =>
      createScreen(effectiveTheatreId, {
        screenName: newScreenName.trim(),
        totalCapacity: parseInt(newScreenCapacity, 10) || 100,
      }),
    onSuccess: () => {
      toast.success('Auditorium screen added successfully');
      setNewScreenModalOpen(false);
      setNewScreenName('');
      refetchScreens();
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to add screen');
    },
  });

  // Create Show Mutation
  const createShowMutation = useMutation({
    mutationFn: () => {
      if (!newShowMovieId || !effectiveScreenId) {
        throw new Error('Please select both a movie and a screen.');
      }
      const startIso = `${newShowDate}T${newShowStartTime}:00+05:30`;
      const endIso = `${newShowDate}T${newShowEndTime}:00+05:30`;

      return createShow({
        theatreId: effectiveTheatreId,
        screenId: effectiveScreenId,
        movieId: newShowMovieId,
        startTime: startIso,
        endTime: endIso,
        format: newShowFormat,
        language: newShowLanguage,
      });
    },
    onSuccess: () => {
      toast.success('Show scheduled successfully');
      setNewShowModalOpen(false);
      refetchShows();
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to schedule show');
    },
  });

  // Update Show Pricing Mutation
  const updatePricingMutation = useMutation({
    mutationFn: () => {
      const pricingList = [
        { seatCategory: 'VIP', price: parseFloat(vipPrice) },
        { seatCategory: 'PREMIUM', price: parseFloat(premiumPrice) },
        { seatCategory: 'STANDARD', price: parseFloat(standardPrice) },
      ];
      return updateShowPricing(effectiveShowId, pricingList);
    },
    onSuccess: () => {
      toast.success('Show category pricing updated');
      setPricingEditOpen(false);
      refetchPricing();
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to update pricing');
    },
  });

  // Inventory Occupancy Calculations
  const inventoryStats = useMemo(() => {
    if (!Array.isArray(seatInventory) || seatInventory.length === 0) {
      return { total: 0, available: 0, held: 0, booked: 0, occupancyPct: 0 };
    }
    const total = seatInventory.length;
    const available = seatInventory.filter((s) => s.status === 'AVAILABLE').length;
    const held = seatInventory.filter((s) => s.status === 'HELD').length;
    const booked = seatInventory.filter((s) => s.status === 'BOOKED').length;
    const occupied = held + booked;
    const occupancyPct = total > 0 ? Math.round((occupied / total) * 100) : 0;
    return { total, available, held, booked, occupancyPct };
  }, [seatInventory]);

  return (
    <div className="min-h-[85vh] bg-[#0A0D14] text-white py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Portal Header */}
        <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <Building2 className="w-6 h-6 text-red-500" />
              <h1 className="text-2xl font-extrabold tracking-tight text-white">
                Theatre Operator Portal
              </h1>
              <Badge variant="warning" className="text-[11px] font-bold">
                OPERATOR SCOPE
              </Badge>
            </div>
            <p className="text-xs text-zinc-400">
              Manage screens, seat configurations, showtimes, pricing, and live hall occupancy.
            </p>
          </div>

          {/* Theatre Selector Dropdown */}
          <div className="flex items-center gap-3">
            <span className="text-xs text-zinc-400 font-medium">Assigned Theatre:</span>
            <select
              value={selectedTheatreId || effectiveTheatreId}
              onChange={(e) => {
                setSelectedTheatreId(e.target.value);
                setSelectedScreenId('');
                setSelectedShowId('');
              }}
              className="h-10 px-3 rounded-xl border border-white/15 bg-white/[0.05] text-white text-xs font-semibold outline-none focus:border-red-500"
            >
              {theatres.map((t) => (
                <option key={t.theatreId} value={t.theatreId} className="bg-zinc-900 text-white">
                  {t.name} ({t.city})
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="border-b border-white/10 overflow-x-auto pb-1">
          <Tabs tabs={OPERATOR_TABS} activeTab={activeTab} onChange={setActiveTab} />
        </div>

        {/* TAB 1: THEATRES & SCREENS */}
        {activeTab === 'THEATRES_SCREENS' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Theatre Info Card */}
            <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-4">
              <h2 className="text-base font-bold text-white border-b border-white/10 pb-3 flex items-center justify-between">
                <span>Theatre Overview</span>
                <Badge variant="success">ACTIVE</Badge>
              </h2>

              <div className="space-y-3 text-xs text-zinc-300">
                <div>
                  <span className="text-zinc-500 block">Theatre Name</span>
                  <span className="font-semibold text-white text-sm">{activeTheatre?.name}</span>
                </div>
                <div>
                  <span className="text-zinc-500 block">City / Market</span>
                  <span className="font-medium text-white">{activeTheatre?.city}</span>
                </div>
                <div>
                  <span className="text-zinc-500 block">Street Address</span>
                  <span className="text-zinc-300">{activeTheatre?.address || 'City Centre'}</span>
                </div>
                <div>
                  <span className="text-zinc-500 block">Total Configured Screens</span>
                  <span className="font-bold text-red-400 text-sm">{screens.length} Screens</span>
                </div>
              </div>
            </div>

            {/* Screens List */}
            <div className="lg:col-span-2 rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-4">
              <div className="flex items-center justify-between border-b border-white/10 pb-3">
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Tv className="w-4 h-4 text-red-500" />
                  <span>Configured Auditorium Screens</span>
                </h2>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => setNewScreenModalOpen(true)}
                  className="gap-1.5 bg-red-600 hover:bg-red-500 text-xs"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add Screen</span>
                </Button>
              </div>

              {isScreensLoading ? (
                <LoadingState message="Loading screens..." />
              ) : screens.length === 0 ? (
                <p className="text-xs text-zinc-500 py-6 text-center">
                  No screens configured for this cinema yet. Click Add Screen above.
                </p>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {screens.map((scr) => (
                    <div
                      key={scr.screenId}
                      className="p-4 rounded-xl border border-white/10 bg-white/[0.02] flex items-center justify-between hover:border-white/20"
                    >
                      <div>
                        <h3 className="font-bold text-white text-sm">{scr.screenName}</h3>
                        <p className="text-xs text-zinc-400 mt-0.5">
                          Capacity: {scr.totalCapacity || 'Standard'} seats
                        </p>
                      </div>
                      <Badge variant="default" className="text-[10px]">
                        SCREEN ID: {scr.screenId.substring(0, 6)}
                      </Badge>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* TAB 2: PHYSICAL SEATS LAYOUT */}
        {activeTab === 'PHYSICAL_SEATS' && (
          <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
              <div>
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Layers className="w-4 h-4 text-red-500" />
                  <span>Auditorium Physical Seat Configuration</span>
                </h2>
                <p className="text-xs text-zinc-400 mt-0.5">
                  Verify the physical seat blueprint and category mapping.
                </p>
              </div>

              {/* Screen Selector */}
              <div className="flex items-center gap-2">
                <span className="text-xs text-zinc-400">Screen:</span>
                <select
                  value={selectedScreenId || effectiveScreenId}
                  onChange={(e) => setSelectedScreenId(e.target.value)}
                  className="h-9 px-3 rounded-lg border border-white/15 bg-white/[0.05] text-white text-xs font-semibold outline-none"
                >
                  {screens.map((s) => (
                    <option key={s.screenId} value={s.screenId} className="bg-zinc-900 text-white">
                      {s.screenName}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {isSeatsLoading ? (
              <LoadingState message="Loading physical seat layout..." />
            ) : physicalSeats.length === 0 ? (
              <p className="text-xs text-zinc-500 py-8 text-center">
                No physical seats configured for this screen.
              </p>
            ) : (
              <div className="space-y-4">
                <div className="flex flex-wrap gap-4 text-xs text-zinc-400">
                  <span>
                    Total Seats: <strong className="text-white">{physicalSeats.length}</strong>
                  </span>
                  <span>
                    VIP: <strong className="text-amber-400">{physicalSeats.filter((s) => s.seatCategory === 'VIP').length}</strong>
                  </span>
                  <span>
                    Premium: <strong className="text-rose-400">{physicalSeats.filter((s) => s.seatCategory === 'PREMIUM').length}</strong>
                  </span>
                  <span>
                    Standard: <strong className="text-zinc-200">{physicalSeats.filter((s) => s.seatCategory === 'STANDARD').length}</strong>
                  </span>
                </div>

                {/* Seat Matrix Visualizer */}
                <div className="rounded-xl border border-white/10 bg-black/40 p-6 overflow-x-auto">
                  <div className="flex flex-wrap gap-2 max-w-3xl mx-auto justify-center">
                    {physicalSeats.map((seat) => (
                      <div
                        key={seat.physicalSeatId}
                        className={`w-9 h-9 rounded-lg flex flex-col items-center justify-center text-[10px] font-bold border transition ${
                          seat.seatCategory === 'VIP'
                            ? 'bg-amber-500/10 border-amber-500/40 text-amber-300'
                            : seat.seatCategory === 'PREMIUM'
                            ? 'bg-rose-500/10 border-rose-500/40 text-rose-300'
                            : 'bg-white/5 border-white/15 text-zinc-300'
                        }`}
                        title={`${seat.seatLabel} (${seat.seatCategory})`}
                      >
                        <span>{seat.seatLabel}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 3: SHOW SCHEDULING */}
        {activeTab === 'SHOW_SCHEDULE' && (
          <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-6">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div>
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Film className="w-4 h-4 text-red-500" />
                  <span>Scheduled Cinema Shows</span>
                </h2>
                <p className="text-xs text-zinc-400 mt-0.5">
                  Publish showtimes and connect movies with auditoriums.
                </p>
              </div>

              <Button
                variant="primary"
                size="sm"
                onClick={() => {
                  if (movies.length > 0 && !newShowMovieId) {
                    setNewShowMovieId(movies[0].movieId);
                  }
                  setNewShowModalOpen(true);
                }}
                className="gap-1.5 bg-red-600 hover:bg-red-500 text-xs"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Schedule New Show</span>
              </Button>
            </div>

            {isShowsLoading ? (
              <LoadingState message="Loading shows..." />
            ) : shows.length === 0 ? (
              <p className="text-xs text-zinc-500 py-8 text-center">
                No shows currently scheduled. Click Schedule New Show to publish a showtime.
              </p>
            ) : (
              <div className="space-y-3">
                {shows.map((sh) => {
                  const m = movies.find((mov) => mov.movieId === sh.movieId);
                  const scr = screens.find((s) => s.screenId === sh.screenId);
                  const dt = sh.startTime ? new Date(sh.startTime) : null;

                  return (
                    <div
                      key={sh.showId}
                      className="p-4 rounded-xl border border-white/10 bg-white/[0.02] flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:border-white/20"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <h3 className="font-bold text-white text-base">
                            {m?.title || 'Scheduled Movie Show'}
                          </h3>
                          <Badge variant="outline" className="text-[10px]">
                            {sh.format || '2D'}
                          </Badge>
                          <Badge variant="outline" className="text-[10px]">
                            {sh.language || 'Telugu'}
                          </Badge>
                        </div>

                        <div className="flex flex-wrap items-center gap-3 text-xs text-zinc-400">
                          <span>{scr?.screenName || 'Screen 1'}</span>
                          <span>&bull;</span>
                          <span>
                            {dt
                              ? dt.toLocaleDateString('en-IN', {
                                  weekday: 'short',
                                  day: 'numeric',
                                  month: 'short',
                                })
                              : 'Date'}
                          </span>
                          <span>&bull;</span>
                          <span className="text-white font-semibold">
                            {dt
                              ? dt.toLocaleTimeString('en-IN', {
                                  hour: '2-digit',
                                  minute: '2-digit',
                                  hour12: true,
                                })
                              : 'Time'}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 self-start sm:self-auto">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setSelectedShowId(sh.showId);
                            setActiveTab('PRICING_POLICY');
                          }}
                          className="border-white/20 text-xs"
                        >
                          Pricing &amp; Policy
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setSelectedShowId(sh.showId);
                            setActiveTab('LIVE_OCCUPANCY');
                          }}
                          className="border-white/20 text-xs text-red-400 hover:text-red-300"
                        >
                          Occupancy
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* TAB 4: PRICING & CANCELLATION POLICY */}
        {activeTab === 'PRICING_POLICY' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Category Pricing Card */}
            <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-5">
              <div className="flex items-center justify-between border-b border-white/10 pb-3">
                <div>
                  <h2 className="text-base font-bold text-white flex items-center gap-2">
                    <DollarSign className="w-4 h-4 text-emerald-400" />
                    <span>Show Category Pricing</span>
                  </h2>
                  <p className="text-xs text-zinc-400 mt-0.5">Authoritative seat pricing rules</p>
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPricingEditOpen(true)}
                  className="gap-1 border-white/20 text-xs"
                >
                  <Edit className="w-3.5 h-3.5" />
                  <span>Edit Pricing</span>
                </Button>
              </div>

              {showPricing.length === 0 ? (
                <p className="text-xs text-zinc-500 py-6 text-center">
                  No custom pricing assigned. Click Edit Pricing to set category rates.
                </p>
              ) : (
                <div className="space-y-3">
                  {showPricing.map((pr) => (
                    <div
                      key={pr.showPricingId || pr.seatCategory}
                      className="p-3.5 rounded-xl border border-white/10 bg-white/[0.02] flex items-center justify-between"
                    >
                      <span className="font-semibold text-white text-sm">{pr.seatCategory}</span>
                      <span className="font-mono font-bold text-emerald-400">
                        INR {Number(pr.price).toLocaleString('en-IN')}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Cancellation Policy Card */}
            <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-5">
              <div className="border-b border-white/10 pb-3">
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Shield className="w-4 h-4 text-red-500" />
                  <span>Cancellation Policy</span>
                </h2>
                <p className="text-xs text-zinc-400 mt-0.5">Show refund deadline rules</p>
              </div>

              <div className="space-y-3 text-xs text-zinc-300">
                <div className="flex justify-between items-center p-3 rounded-xl bg-white/[0.02] border border-white/10">
                  <span>Policy Type:</span>
                  <Badge variant="success">{cancellationPolicy?.policyType || 'STANDARD'}</Badge>
                </div>

                {cancellationPolicy?.cancellationDeadline && (
                  <div className="flex justify-between items-center p-3 rounded-xl bg-white/[0.02] border border-white/10">
                    <span>Cancellation Deadline:</span>
                    <span className="font-mono text-zinc-200">
                      {new Date(cancellationPolicy.cancellationDeadline).toLocaleString('en-IN')}
                    </span>
                  </div>
                )}

                <div className="flex justify-between items-center p-3 rounded-xl bg-white/[0.02] border border-white/10">
                  <span>Policy Status:</span>
                  <Badge variant="outline">{cancellationPolicy?.status || 'ACTIVE'}</Badge>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 5: LIVE OCCUPANCY MONITOR */}
        {activeTab === 'LIVE_OCCUPANCY' && (
          <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
              <div>
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Percent className="w-4 h-4 text-red-500" />
                  <span>Auditorium Live Occupancy</span>
                </h2>
                <p className="text-xs text-zinc-400 mt-0.5">
                  Real-time seat inventory monitor with automatic 5s refresh.
                </p>
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => refetchInventory()}
                className="gap-1.5 border-white/20 text-xs self-start sm:self-auto"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Refresh Now</span>
              </Button>
            </div>

            {/* Metrics KPI Cards */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div className="p-4 rounded-xl border border-white/10 bg-white/[0.02]">
                <span className="text-xs text-zinc-500 block">Total Capacity</span>
                <span className="text-2xl font-bold font-mono text-white">
                  {inventoryStats.total}
                </span>
              </div>
              <div className="p-4 rounded-xl border border-white/10 bg-white/[0.02]">
                <span className="text-xs text-zinc-500 block">Available</span>
                <span className="text-2xl font-bold font-mono text-emerald-400">
                  {inventoryStats.available}
                </span>
              </div>
              <div className="p-4 rounded-xl border border-white/10 bg-white/[0.02]">
                <span className="text-xs text-zinc-500 block">In 5-Min Hold</span>
                <span className="text-2xl font-bold font-mono text-amber-400">
                  {inventoryStats.held}
                </span>
              </div>
              <div className="p-4 rounded-xl border border-white/10 bg-white/[0.02]">
                <span className="text-xs text-zinc-500 block">Occupancy</span>
                <span className="text-2xl font-bold font-mono text-red-400">
                  {inventoryStats.occupancyPct}%
                </span>
              </div>
            </div>

            {/* Live Inventory Seats Matrix */}
            <div className="rounded-xl border border-white/10 bg-black/40 p-6 overflow-x-auto">
              <div className="flex flex-wrap gap-2 max-w-4xl mx-auto justify-center">
                {seatInventory.map((item) => (
                  <div
                    key={item.showSeatInventoryId}
                    className={`w-9 h-9 rounded-lg flex flex-col items-center justify-center text-[10px] font-bold border ${
                      item.status === 'AVAILABLE'
                        ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                        : item.status === 'HELD'
                        ? 'bg-amber-500/20 border-amber-500/40 text-amber-300 animate-pulse'
                        : 'bg-zinc-800 border-zinc-700 text-zinc-600 line-through'
                    }`}
                    title={`Seat Status: ${item.status}`}
                  >
                    <span>{item.seatLabelSnapshot || 'S'}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* MODAL: ADD AUDITORIUM SCREEN */}
        <Modal
          isOpen={newScreenModalOpen}
          onClose={() => setNewScreenModalOpen(false)}
          title="Add Auditorium Screen"
        >
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-400">Screen Name</label>
              <input
                type="text"
                placeholder="e.g. Audi 1 (Dolby Atmos)"
                value={newScreenName}
                onChange={(e) => setNewScreenName(e.target.value)}
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-400">Seat Capacity</label>
              <input
                type="number"
                value={newScreenCapacity}
                onChange={(e) => setNewScreenCapacity(e.target.value)}
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>

            <div className="flex justify-end gap-3 pt-3">
              <Button variant="outline" onClick={() => setNewScreenModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => createScreenMutation.mutate()}
                isLoading={createScreenMutation.isPending}
                className="bg-red-600 hover:bg-red-500"
              >
                Create Screen
              </Button>
            </div>
          </div>
        </Modal>

        {/* MODAL: SCHEDULE NEW SHOW */}
        <Modal
          isOpen={newShowModalOpen}
          onClose={() => setNewShowModalOpen(false)}
          title="Schedule New Movie Show"
        >
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-400">Movie</label>
              <select
                value={newShowMovieId}
                onChange={(e) => setNewShowMovieId(e.target.value)}
                className="w-full h-11 px-3 rounded-xl border border-white/10 bg-[#121620] text-white text-sm outline-none"
              >
                {movies.map((m) => (
                  <option key={m.movieId} value={m.movieId}>
                    {m.title} ({m.language})
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">Show Date</label>
                <input
                  type="date"
                  value={newShowDate}
                  onChange={(e) => setNewShowDate(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">Auditorium</label>
                <select
                  value={effectiveScreenId}
                  onChange={(e) => setSelectedScreenId(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-[#121620] text-white text-sm outline-none"
                >
                  {screens.map((s) => (
                    <option key={s.screenId} value={s.screenId}>
                      {s.screenName}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">Start Time</label>
                <input
                  type="time"
                  value={newShowStartTime}
                  onChange={(e) => setNewShowStartTime(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">End Time</label>
                <input
                  type="time"
                  value={newShowEndTime}
                  onChange={(e) => setNewShowEndTime(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">Format</label>
                <select
                  value={newShowFormat}
                  onChange={(e) => setNewShowFormat(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-[#121620] text-white text-sm outline-none"
                >
                  <option value="2D">2D Standard</option>
                  <option value="3D">3D RealD</option>
                  <option value="IMAX">IMAX 3D</option>
                  <option value="4DX">4DX Experience</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">Language</label>
                <input
                  type="text"
                  value={newShowLanguage}
                  onChange={(e) => setNewShowLanguage(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-3">
              <Button variant="outline" onClick={() => setNewShowModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => createShowMutation.mutate()}
                isLoading={createShowMutation.isPending}
                className="bg-red-600 hover:bg-red-500"
              >
                Publish Show
              </Button>
            </div>
          </div>
        </Modal>

        {/* MODAL: EDIT SHOW PRICING */}
        <Modal
          isOpen={pricingEditOpen}
          onClose={() => setPricingEditOpen(false)}
          title="Configure Seat Category Pricing"
        >
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-amber-400">VIP Seats Price (INR)</label>
              <input
                type="number"
                value={vipPrice}
                onChange={(e) => setVipPrice(e.target.value)}
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-rose-400">PREMIUM Seats Price (INR)</label>
              <input
                type="number"
                value={premiumPrice}
                onChange={(e) => setPremiumPrice(e.target.value)}
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-300">STANDARD Seats Price (INR)</label>
              <input
                type="number"
                value={standardPrice}
                onChange={(e) => setStandardPrice(e.target.value)}
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>

            <div className="flex justify-end gap-3 pt-3">
              <Button variant="outline" onClick={() => setPricingEditOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => updatePricingMutation.mutate()}
                isLoading={updatePricingMutation.isPending}
                className="bg-red-600 hover:bg-red-500"
              >
                Save Category Pricing
              </Button>
            </div>
          </div>
        </Modal>
      </div>
    </div>
  );
}

export default OperatorPortalPage;
