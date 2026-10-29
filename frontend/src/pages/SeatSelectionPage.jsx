import { useState, useMemo, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ChevronLeft,
  X,
  AlertCircle,
  ZoomIn,
  ZoomOut,
  RotateCcw,
} from 'lucide-react';
import { useAuth } from '../context/useAuth';
import { useToast } from '../context/useToast';
import {
  fetchShowById,
  fetchShowPricing,
  fetchTheatresByCity,
  fetchPhysicalSeatsByScreen,
} from '../features/shows/api/showsAndTheatresApi';
import { useMovieDetail } from '../features/movies/hooks/useMovieDetail';
import {
  fetchShowSeatInventory,
  createSeatHold,
} from '../features/seats/api/seatsApi';
import { ProgressIndicator } from '../components/ui/ProgressIndicator';
import { SeatLegend } from '../components/ui/SeatLegend';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { LoadingState } from '../components/ui/LoadingState';
import { ErrorState } from '../components/ui/ErrorState';

const MAX_SEATS_PER_BOOKING = 6;

function formatShowTime(isoString) {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    return d.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    return isoString;
  }
}

function formatShowDate(isoString) {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    return d.toLocaleDateString('en-IN', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
    });
  } catch {
    return isoString;
  }
}

export function SeatSelectionPage() {
  const { showId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const { isAuthenticated } = useAuth();

  const [selectedSeatIds, setSelectedSeatIds] = useState(() => {
    try {
      const saved = sessionStorage.getItem('bookngo_pending_booking');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (parsed.showId === showId && Array.isArray(parsed.selectedSeatIds)) {
          return parsed.selectedSeatIds;
        }
      }
    } catch {
      // ignore
    }
    return [];
  });
  const [zoomLevel, setZoomLevel] = useState(1);
  const [conflictModalOpen, setConflictModalOpen] = useState(false);

  // 1. Fetch Show Details
  const {
    data: show,
    isLoading: loadingShow,
    error: showError,
  } = useQuery({
    queryKey: ['show', showId],
    queryFn: ({ signal }) => fetchShowById(showId, { signal }),
    enabled: Boolean(showId),
  });

  const screenId = show?.screenId;
  const movieId = show?.movieId;
  const theatreId = show?.theatreId;

  // 2. Fetch Movie Details
  const { data: movie } = useMovieDetail(movieId);

  // 3. Fetch Theatres (to find current theatre metadata)
  const { data: theatres = [] } = useQuery({
    queryKey: ['theatres'],
    queryFn: ({ signal }) => fetchTheatresByCity('', { signal }),
    staleTime: 5 * 60 * 1000,
  });

  const currentTheatre = useMemo(() => {
    return theatres.find((t) => t.theatreId === theatreId) || null;
  }, [theatres, theatreId]);

  // 4. Fetch Physical Seats for Screen
  const {
    data: physicalSeats = [],
    isLoading: loadingPhysicalSeats,
    error: physicalSeatsError,
  } = useQuery({
    queryKey: ['screenSeats', screenId],
    queryFn: ({ signal }) => fetchPhysicalSeatsByScreen(screenId, { signal }),
    enabled: Boolean(screenId),
    staleTime: 10 * 60 * 1000,
  });

  // 5. Fetch Show Pricing
  const {
    data: pricingList = [],
    isLoading: loadingPricing,
  } = useQuery({
    queryKey: ['showPricing', showId],
    queryFn: ({ signal }) => fetchShowPricing(showId, { signal }),
    enabled: Boolean(showId),
  });

  // Map category -> price
  const pricingMap = useMemo(() => {
    const map = new Map();
    pricingList.forEach((p) => {
      map.set(p.seatCategory.toUpperCase(), p.amount);
    });
    return map;
  }, [pricingList]);

  // 6. Fetch Real-Time Seat Inventory with 5s Polling
  const {
    data: seatInventory = [],
    error: inventoryError,
  } = useQuery({
    queryKey: ['seatInventory', showId],
    queryFn: ({ signal }) => fetchShowSeatInventory(showId, { signal }),
    enabled: Boolean(showId),
    refetchInterval: 5000, // Background poll every 5s per UX specification
  });

  // Map physicalSeatId -> inventory item
  const inventoryMap = useMemo(() => {
    const map = new Map();
    seatInventory.forEach((item) => {
      map.set(item.physicalSeatId, item);
    });
    return map;
  }, [seatInventory]);

  // Derived active selection: automatically drops seats that became unavailable in live inventory
  const activeSelectedSeatIds = useMemo(() => {
    return selectedSeatIds.filter((seatId) => {
      const inv = inventoryMap.get(seatId);
      return !inv || inv.status === 'AVAILABLE';
    });
  }, [selectedSeatIds, inventoryMap]);

  // Organize physical seats into rows and tiers
  const seatGridData = useMemo(() => {
    if (!physicalSeats || physicalSeats.length === 0) return [];

    const rowsMap = new Map();
    physicalSeats.forEach((seat) => {
      const row = seat.rowLabel.toUpperCase();
      if (!rowsMap.has(row)) {
        rowsMap.set(row, {
          rowLabel: row,
          category: seat.seatCategory,
          seats: [],
        });
      }
      rowsMap.get(row).seats.push(seat);
    });

    // Sort rows alphabetically (A, B, C...)
    const sortedRows = Array.from(rowsMap.values()).sort((a, b) =>
      a.rowLabel.localeCompare(b.rowLabel)
    );

    // Sort seats in each row by seatNumber numerically
    sortedRows.forEach((row) => {
      row.seats.sort((a, b) => {
        const numA = parseInt(a.seatNumber, 10);
        const numB = parseInt(b.seatNumber, 10);
        return (isNaN(numA) ? 0 : numA) - (isNaN(numB) ? 0 : numB);
      });
    });

    return sortedRows;
  }, [physicalSeats]);

  // Group tiers for header dividers and legend
  const categoryTiers = useMemo(() => {
    const list = [];
    pricingList.forEach((p) => {
      list.push({
        tier: p.seatCategory,
        price: p.amount,
      });
    });
    return list;
  }, [pricingList]);

  // Selected seats objects for price summary
  const selectedSeatObjects = useMemo(() => {
    return activeSelectedSeatIds
      .map((id) => physicalSeats.find((s) => s.physicalSeatId === id))
      .filter(Boolean);
  }, [activeSelectedSeatIds, physicalSeats]);

  // Calculate Running Subtotal
  const runningTotal = useMemo(() => {
    let sum = 0;
    selectedSeatObjects.forEach((seat) => {
      const price = pricingMap.get(seat.seatCategory.toUpperCase()) || 200;
      sum += price;
    });
    return sum;
  }, [selectedSeatObjects, pricingMap]);

  // Toggle Seat Selection (Local Draft Only)
  const handleToggleSeat = useCallback(
    (seat) => {
      const { physicalSeatId } = seat;
      const inv = inventoryMap.get(physicalSeatId);
      const isAvailable = !inv || inv.status === 'AVAILABLE';

      if (!isAvailable) return;

      const isSelected = selectedSeatIds.includes(physicalSeatId);

      if (isSelected) {
        setSelectedSeatIds((prev) => prev.filter((id) => id !== physicalSeatId));
      } else {
        if (selectedSeatIds.length >= MAX_SEATS_PER_BOOKING) {
          toast.warning(`Maximum ${MAX_SEATS_PER_BOOKING} seats allowed per booking.`);
          return;
        }
        setSelectedSeatIds((prev) => [...prev, physicalSeatId]);
      }
    },
    [inventoryMap, selectedSeatIds, toast]
  );

  // Mutation to acquire 5-minute atomic server hold
  const holdMutation = useMutation({
    mutationFn: async () => {
      const idempotencyKey = crypto.randomUUID();
      return createSeatHold(showId, activeSelectedSeatIds, idempotencyKey);
    },
    onSuccess: (data) => {
      // Clear pending draft
      sessionStorage.removeItem('bookngo_pending_booking');
      // Invalidate inventory
      queryClient.invalidateQueries({ queryKey: ['seatInventory', showId] });
      // Navigate to checkout with bookingId
      navigate(`/checkout/${data.bookingId}`, {
        state: { holdResponse: data },
      });
    },
    onError: (err) => {
      if (err.status === 409) {
        setConflictModalOpen(true);
        queryClient.invalidateQueries({ queryKey: ['seatInventory', showId] });
      } else {
        toast.error(err.message || 'Unable to reserve seats. Please try again.');
      }
    },
  });

  // Handle "Proceed to Book" CTA
  const handleProceedToBook = () => {
    if (activeSelectedSeatIds.length === 0) return;

    if (!isAuthenticated) {
      // Save pending draft in sessionStorage
      sessionStorage.setItem(
        'bookngo_pending_booking',
        JSON.stringify({ showId, selectedSeatIds: activeSelectedSeatIds })
      );
      toast.info('Please verify your phone number with OTP to reserve your seats.');
      navigate(`/login?returnUrl=/shows/${showId}/seats`);
      return;
    }

    holdMutation.mutate();
  };

  const isLoadingInitial = loadingShow || loadingPhysicalSeats || loadingPricing;

  if (isLoadingInitial) {
    return (
      <div className="w-full min-h-screen bg-[#0B0F17] flex items-center justify-center">
        <LoadingState
          title="Loading Cinema Auditorium..."
          message="Synchronizing physical screen layout and real-time inventory"
          variant="full"
        />
      </div>
    );
  }

  if (showError || physicalSeatsError || inventoryError) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <ErrorState
          title="Unable to Load Seat Inventory"
          message={
            showError?.message ||
            physicalSeatsError?.message ||
            inventoryError?.message ||
            'Failed to establish connection with BookNGo Booking Service.'
          }
          onRetry={() => {
            queryClient.invalidateQueries({ queryKey: ['show', showId] });
            queryClient.invalidateQueries({ queryKey: ['screenSeats', screenId] });
            queryClient.invalidateQueries({ queryKey: ['seatInventory', showId] });
          }}
        />
      </div>
    );
  }

  return (
    <div className="w-full min-h-screen bg-[#0B0F17] text-gray-100 flex flex-col pb-32">
      {/* 1. BOOKING PROGRESS WIZARD */}
      <ProgressIndicator currentStep={2} />

      {/* 2. SHOW & CINEMA HEADER */}
      <header className="bg-slate-950/80 border-b border-slate-800/80 px-4 sm:px-6 lg:px-8 py-4">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <Link
              to={`/movies/${movieId}/shows`}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white hover:border-slate-700 transition"
              aria-label="Back to showtimes"
            >
              <ChevronLeft className="w-4 h-4" />
            </Link>

            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base sm:text-lg font-bold text-white tracking-tight">
                  {movie?.title || 'Movie'}
                </h1>
                {show?.showFormat && (
                  <Badge variant="default" size="xs">
                    {show.showFormat}
                  </Badge>
                )}
              </div>

              <div className="flex flex-wrap items-center gap-2 text-xs text-slate-400 mt-0.5">
                <span className="text-slate-200 font-semibold">
                  {currentTheatre?.name || 'PVR Cinema'}
                </span>
                <span className="text-slate-600">•</span>
                <span>{formatShowDate(show?.startsAt)}</span>
                <span className="text-slate-600">•</span>
                <span className="text-red-400 font-bold">
                  {formatShowTime(show?.startsAt)}
                </span>
              </div>
            </div>
          </div>

          {/* Zoom controls */}
          <div className="flex items-center gap-1.5 self-end sm:self-auto bg-slate-900/90 border border-slate-800 px-2 py-1 rounded-xl">
            <button
              onClick={() => setZoomLevel((z) => Math.max(0.7, z - 0.15))}
              className="p-1 text-slate-400 hover:text-white transition rounded"
              title="Zoom Out"
            >
              <ZoomOut className="w-4 h-4" />
            </button>
            <span className="text-[11px] font-mono font-semibold px-1 text-slate-300">
              {Math.round(zoomLevel * 100)}%
            </span>
            <button
              onClick={() => setZoomLevel((z) => Math.min(1.4, z + 0.15))}
              className="p-1 text-slate-400 hover:text-white transition rounded"
              title="Zoom In"
            >
              <ZoomIn className="w-4 h-4" />
            </button>
            <button
              onClick={() => setZoomLevel(1)}
              className="p-1 text-slate-400 hover:text-white transition rounded ml-1 border-l border-slate-800"
              title="Reset View"
            >
              <RotateCcw className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </header>

      {/* 3. SEAT LEGEND BAR */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4 w-full">
        <SeatLegend categories={categoryTiers} currencySymbol="₹" />
      </div>

      {/* 4. MAIN AUDITORIUM CANVAS */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col items-center overflow-x-auto no-scrollbar">
        {/* Cinema Screen Curved Arc */}
        <div className="w-full max-w-2xl mb-12 flex flex-col items-center select-none">
          <div className="relative w-full h-8 flex items-center justify-center">
            {/* Glowing Curved SVG Screen */}
            <svg
              className="w-full h-12 text-red-500 drop-shadow-[0_0_12px_rgba(229,9,20,0.6)]"
              viewBox="0 0 600 40"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M10 35 C 200 5, 400 5, 590 35"
                stroke="currentColor"
                strokeWidth="4"
                strokeLinecap="round"
              />
            </svg>
          </div>
          <span className="text-[11px] font-black uppercase tracking-[0.25em] text-slate-400 mt-2">
            Screen This Way
          </span>
        </div>

        {/* Seat Map Scalable Container */}
        <div
          style={{ transform: `scale(${zoomLevel})`, transformOrigin: 'top center' }}
          className="transition-transform duration-150 ease-out space-y-4 pb-8"
        >
          {seatGridData.map((rowGroup, rowIndex) => {
            const rowPrice = pricingMap.get(rowGroup.category.toUpperCase()) || 200;

            // Optional category divider label between tiers
            const prevRow = rowIndex > 0 ? seatGridData[rowIndex - 1] : null;
            const isNewCategory = !prevRow || prevRow.category !== rowGroup.category;

            return (
              <div key={rowGroup.rowLabel} className="space-y-2">
                {isNewCategory && (
                  <div className="text-center pt-4 pb-1">
                    <span className="text-[10px] font-black uppercase tracking-wider text-slate-500 bg-slate-900/90 border border-slate-800 px-3 py-1 rounded-full">
                      {rowGroup.category} Tier • ₹{rowPrice}
                    </span>
                  </div>
                )}

                <div className="flex items-center justify-center gap-3">
                  {/* Left Row Identifier */}
                  <span className="w-6 text-center text-xs font-bold text-slate-500 select-none">
                    {rowGroup.rowLabel}
                  </span>

                  {/* Seat Row Grid */}
                  <div className="flex items-center gap-2">
                    {rowGroup.seats.map((seat, seatIdx) => {
                      const { physicalSeatId, seatNumber } = seat;
                      const inv = inventoryMap.get(physicalSeatId);
                      const status = inv ? inv.status : 'AVAILABLE';
                      const isSelected = selectedSeatIds.includes(physicalSeatId);

                      // Aisle gap simulation: add visual gap after every 4th or 6th seat
                      const isAisleGap = seatIdx === 4 || seatIdx === 10;

                      return (
                        <div key={physicalSeatId} className="flex items-center">
                          {isAisleGap && <div className="w-5 sm:w-6" />}
                          <button
                            type="button"
                            onClick={() => handleToggleSeat(seat)}
                            disabled={status !== 'AVAILABLE'}
                            aria-label={`Row ${rowGroup.rowLabel} Seat ${seatNumber}, ${rowGroup.category}, ₹${rowPrice}`}
                            className={`w-7 h-7 sm:w-8 sm:h-8 rounded-lg text-[10px] sm:text-xs font-bold transition-all duration-150 flex items-center justify-center cursor-pointer select-none ${
                              isSelected
                                ? 'bg-red-600 text-white border-2 border-red-400 shadow-md shadow-red-600/40 scale-105 z-10'
                                : status === 'BOOKED'
                                  ? 'bg-slate-800/40 text-slate-600 border border-transparent cursor-not-allowed'
                                  : status === 'HELD'
                                    ? 'border-2 border-amber-500/60 bg-amber-950/30 text-amber-500/60 cursor-not-allowed'
                                    : 'bg-slate-900 border-2 border-slate-700/80 text-slate-300 hover:border-red-500 hover:text-white hover:scale-105'
                            }`}
                          >
                            {seatNumber}
                          </button>
                        </div>
                      );
                    })}
                  </div>

                  {/* Right Row Identifier */}
                  <span className="w-6 text-center text-xs font-bold text-slate-500 select-none">
                    {rowGroup.rowLabel}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </main>

      {/* 5. STICKY BOTTOM SELECTION SUMMARY DRAWER */}
      {selectedSeatIds.length > 0 && (
        <aside
          aria-label="Selected Seats Summary"
          className="fixed bottom-0 inset-x-0 z-40 bg-slate-950/95 border-t border-slate-800 backdrop-blur-md shadow-2xl py-3 px-4 sm:px-6 lg:px-8 animate-in slide-in-from-bottom duration-200"
        >
          <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
            {/* Left: Selected seat pills */}
            <div className="flex items-center gap-3 overflow-x-auto w-full sm:w-auto">
              <div className="shrink-0">
                <span className="text-xs font-bold text-white block">
                  {selectedSeatIds.length} of {MAX_SEATS_PER_BOOKING} Seats
                </span>
                <span className="text-[10px] text-slate-400">Selected</span>
              </div>

              <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar">
                {selectedSeatObjects.map((seat) => (
                  <span
                    key={seat.physicalSeatId}
                    className="inline-flex items-center gap-1 bg-red-600 text-white text-xs font-bold px-2.5 py-1 rounded-lg shadow-sm"
                  >
                    <span>
                      {seat.rowLabel}
                      {seat.seatNumber}
                    </span>
                    <button
                      onClick={() => handleToggleSeat(seat)}
                      className="hover:text-red-200 p-0.5 cursor-pointer"
                      aria-label="Remove seat"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                ))}
              </div>
            </div>

            {/* Right: Subtotal & Continue Button */}
            <div className="flex items-center justify-between sm:justify-end gap-5 w-full sm:w-auto shrink-0">
              <div className="text-left sm:text-right">
                <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wide block">
                  Estimated Total
                </span>
                <div className="text-lg sm:text-xl font-black text-white tracking-tight">
                  <span className="text-red-500 mr-0.5">₹</span>
                  {runningTotal}
                </div>
              </div>

              <Button
                variant="primary"
                size="md"
                disabled={holdMutation.isPending}
                isLoading={holdMutation.isPending}
                onClick={handleProceedToBook}
                className="font-bold px-7 rounded-xl shadow-xl shadow-red-600/30 text-sm whitespace-nowrap"
              >
                {holdMutation.isPending ? 'Reserving Seats...' : 'Proceed to Book'}
              </Button>
            </div>
          </div>
        </aside>
      )}

      {/* 6. CONCURRENCY CONFLICT MODAL */}
      <Modal
        isOpen={conflictModalOpen}
        onClose={() => setConflictModalOpen(false)}
        title="Seat Availability Conflict"
        description="One or more selected seats were just acquired by another customer."
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 p-4 rounded-xl bg-amber-950/30 border border-amber-500/40 text-amber-200 text-xs leading-relaxed">
            <AlertCircle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
            <div>
              Because BookNGo uses real-time transactional locking with zero double booking,
              seats are atomically acquired. Please choose alternative seats on the seat map.
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                setConflictModalOpen(false);
                setSelectedSeatIds([]);
              }}
            >
              Choose Alternative Seats
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default SeatSelectionPage;
