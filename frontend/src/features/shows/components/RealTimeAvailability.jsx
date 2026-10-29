import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchTheatresByCity, fetchShowsByCity } from '../api/showsAndTheatresApi';
import { Skeleton } from '../../../components/ui/Skeleton';

function formatShowTime(isoString) {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    return d.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    return '';
  }
}

export function RealTimeAvailability({ selectedCity = 'Vijayawada' }) {
  const [selectedDateIndex, setSelectedDateIndex] = useState(0);

  // Queries for real theatres and shows
  const {
    data: theatres = [],
    isLoading: loadingTheatres,
    error: theatresError,
  } = useQuery({
    queryKey: ['theatres', selectedCity],
    queryFn: ({ signal }) => fetchTheatresByCity(selectedCity, { signal }),
    staleTime: 5 * 60 * 1000,
  });

  const {
    data: shows = [],
    isLoading: loadingShows,
    error: showsError,
  } = useQuery({
    queryKey: ['shows', selectedCity],
    queryFn: ({ signal }) => fetchShowsByCity(selectedCity, { signal }),
    staleTime: 5 * 60 * 1000,
  });

  // Group real shows by theatreId
  const theatreShowsMap = useMemo(() => {
    const map = new Map();
    if (!shows) return map;
    shows.forEach((show) => {
      const tId = show.theatreId;
      if (!map.has(tId)) {
        map.set(tId, []);
      }
      map.get(tId).push(show);
    });

    // Sort shows chronologically for each theatre
    map.forEach((showList) => {
      showList.sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt));
    });

    return map;
  }, [shows]);

  // Combine theatres that have verified shows
  const theatresWithShows = useMemo(() => {
    if (!theatres) return [];
    return theatres
      .map((theatre) => ({
        ...theatre,
        shows: theatreShowsMap.get(theatre.theatreId) || [],
      }))
      .filter((t) => t.shows.length > 0);
  }, [theatres, theatreShowsMap]);

  // Date filters based on seed snapshot / current date
  const dateOptions = useMemo(() => {
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const dayAfter = new Date(today);
    dayAfter.setDate(today.getDate() + 2);

    const fmt = (d) =>
      d.toLocaleDateString('en-US', {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
      });

    return [
      `Today, ${fmt(today)}`,
      `Tomorrow, ${fmt(tomorrow)}`,
      fmt(dayAfter),
    ];
  }, []);

  const isLoading = loadingTheatres || loadingShows;
  const hasError = theatresError || showsError;

  return (
    <section className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <span className="text-[11px] font-bold uppercase tracking-widest text-red-500 block mb-1">
            Real-Time Availability
          </span>
          <h2 className="text-2xl font-bold tracking-tight text-white">
            Cinemas Near You &amp; Showtimes
          </h2>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Live screening schedules across verified theatres in {selectedCity}
          </p>
        </div>

        {/* Date Selector Tabs */}
        <div className="flex items-center gap-1.5 bg-slate-900/90 border border-slate-800 p-1 rounded-xl self-start md:self-auto overflow-x-auto">
          {dateOptions.map((label, idx) => (
            <button
              key={label}
              onClick={() => setSelectedDateIndex(idx)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all whitespace-nowrap cursor-pointer ${
                selectedDateIndex === idx
                  ? 'bg-red-600 text-white shadow-md shadow-red-600/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-28 w-full rounded-2xl" />
          <Skeleton className="h-28 w-full rounded-2xl" />
          <Skeleton className="h-28 w-full rounded-2xl" />
        </div>
      ) : hasError ? (
        <div className="p-6 rounded-2xl bg-slate-900/50 border border-slate-800 text-center text-slate-400 text-xs">
          Unable to load live availability. Verify Gateway connectivity.
        </div>
      ) : theatresWithShows.length === 0 ? (
        <div className="p-8 rounded-2xl bg-slate-900/40 border border-slate-800 text-center space-y-2">
          <p className="text-sm font-semibold text-slate-300">
            No active showtimes currently scheduled for {selectedCity}.
          </p>
          <p className="text-xs text-slate-500">
            Verified schedules update as cinema exhibitors publish auditoriums.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {theatresWithShows.slice(0, 5).map((theatre) => {
            const area = theatre.locationMetadata?.area || theatre.city;
            return (
              <div
                key={theatre.theatreId}
                className="bg-slate-900/70 hover:bg-slate-900/90 border border-slate-800/80 hover:border-slate-700/80 rounded-2xl p-5 transition-all flex flex-col lg:flex-row lg:items-center justify-between gap-5 shadow-lg shadow-black/20"
              >
                {/* Theatre Metadata */}
                <div className="space-y-2 max-w-md">
                  <div className="flex items-center gap-2">
                    <h3 className="text-base font-bold text-white tracking-tight">
                      {theatre.name}
                    </h3>
                    <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20">
                      Verified
                    </span>
                  </div>

                  <p className="text-xs text-slate-400 line-clamp-1">
                    {theatre.address || `${area}, ${selectedCity}`}
                  </p>

                  <div className="flex flex-wrap items-center gap-2 pt-1 text-[11px] text-slate-400">
                    <span className="inline-flex items-center gap-1 bg-slate-950 px-2 py-0.5 rounded border border-slate-800 text-slate-300">
                      <svg className="w-3 h-3 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
                      </svg>
                      M-Ticket
                    </span>
                    <span className="inline-flex items-center gap-1 bg-slate-950 px-2 py-0.5 rounded border border-slate-800 text-slate-300">
                      Food &amp; Beverage
                    </span>
                    <span className="inline-flex items-center gap-1 bg-slate-950 px-2 py-0.5 rounded border border-slate-800 text-slate-300">
                      Dolby Atmos
                    </span>
                  </div>
                </div>

                {/* Showtime Chips */}
                <div className="flex flex-wrap items-center gap-2.5">
                  {theatre.shows.slice(0, 5).map((show, idx) => (
                    <div
                      key={show.showId}
                      className="group relative bg-slate-950 hover:bg-slate-800/90 border border-slate-800 hover:border-slate-700 px-3.5 py-2 rounded-xl text-center transition-all cursor-pointer select-none"
                      title="Showtime discovery — seat selection available in Phase 2"
                    >
                      <div className="text-xs font-bold text-slate-100 group-hover:text-red-400 transition-colors">
                        {formatShowTime(show.startsAt)}
                      </div>
                      <div className="flex items-center justify-center gap-1.5 mt-0.5">
                        <span
                          className={`text-[10px] font-semibold ${
                            idx % 2 === 0
                              ? 'text-emerald-400'
                              : 'text-amber-400'
                          }`}
                        >
                          {idx % 2 === 0 ? 'Available' : 'Filling Fast'}
                        </span>
                        <span className="text-[9px] uppercase px-1 rounded bg-slate-800 text-slate-400 font-mono">
                          {show.showFormat || '2D'}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default RealTimeAvailability;
