import { useState, useMemo } from 'react';
import { useParams, Link, useNavigate, useOutletContext } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import {
  Film,
  MapPin,
  Clock,
  RefreshCw,
} from 'lucide-react';
import { useMovieDetail } from '../features/movies/hooks/useMovieDetail';
import { fetchShows, fetchTheatresByCity } from '../features/shows/api/showsAndTheatresApi';
import { getMoviePosterUrl } from '../features/movies/lib/moviePosters';
import { DateSelector } from '../components/ui/DateSelector';
import { ShowtimeButton } from '../components/ui/ShowtimeButton';
import { Badge } from '../components/ui/Badge';
import { Chip } from '../components/ui/Chip';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { ShowtimeGroupSkeleton } from '../components/ui/Skeleton';

function getTodayIsoString() {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDuration(minutes) {
  if (!minutes || minutes <= 0) return '';
  const hrs = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hrs > 0 && mins > 0) return `${hrs}h ${mins}m`;
  if (hrs > 0) return `${hrs}h`;
  return `${mins}m`;
}

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

export function ShowDiscoveryPage() {
  const { movieId } = useParams();
  const navigate = useNavigate();
  const { selectedCity = 'Vijayawada' } = useOutletContext() || {};

  const [selectedDate, setSelectedDate] = useState(() => getTodayIsoString());
  const [selectedFormat, setSelectedFormat] = useState('ALL');

  // Fetch Movie Details
  const {
    data: movie,
    isLoading: loadingMovie,
  } = useMovieDetail(movieId);

  // Fetch Theatres in current city
  const {
    data: theatres = [],
    isLoading: loadingTheatres,
  } = useQuery({
    queryKey: ['theatres', selectedCity],
    queryFn: ({ signal }) => fetchTheatresByCity(selectedCity, { signal }),
    staleTime: 5 * 60 * 1000,
  });

  // Map theatreId -> theatre object for instant lookup
  const theatreMap = useMemo(() => {
    const map = new Map();
    theatres.forEach((t) => map.set(t.theatreId, t));
    return map;
  }, [theatres]);

  // Fetch Shows for (movieId, selectedDate, selectedCity)
  const {
    data: shows = [],
    isLoading: loadingShows,
    error: showsError,
    refetch: refetchShows,
  } = useQuery({
    queryKey: ['shows', movieId, selectedCity, selectedDate],
    queryFn: ({ signal }) =>
      fetchShows({
        movieId,
        city: selectedCity,
        date: selectedDate,
        signal,
      }),
    enabled: Boolean(movieId),
    staleTime: 30 * 1000,
  });

  // Filter shows by format if selected
  const filteredShows = useMemo(() => {
    if (!shows) return [];
    if (selectedFormat === 'ALL') return shows;
    return shows.filter(
      (s) =>
        s.showFormat &&
        s.showFormat.toUpperCase().includes(selectedFormat.toUpperCase())
    );
  }, [shows, selectedFormat]);

  // Group shows by theatreId
  const theatreGroupedShows = useMemo(() => {
    const groups = new Map();

    filteredShows.forEach((show) => {
      const tid = show.theatreId;
      if (!groups.has(tid)) {
        const theatreInfo = theatreMap.get(tid) || {
          theatreId: tid,
          name: 'Cinema Auditorium',
          address: selectedCity,
          city: selectedCity,
        };
        groups.set(tid, {
          theatre: theatreInfo,
          shows: [],
        });
      }
      groups.get(tid).shows.push(show);
    });

    // Sort shows inside each group chronologically by startsAt
    groups.forEach((group) => {
      group.shows.sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt));
    });

    return Array.from(groups.values());
  }, [filteredShows, theatreMap, selectedCity]);

  const resolvedPosterUrl = movie ? getMoviePosterUrl(movie) : null;

  return (
    <div className="w-full min-h-screen bg-[#0B0F17] text-gray-100 pb-24">
      {/* 1. MOVIE SUMMARY BAR */}
      <section className="bg-slate-950/80 border-b border-slate-800/80 sticky top-14 sm:top-16 z-30 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              {resolvedPosterUrl ? (
                <img
                  src={resolvedPosterUrl}
                  alt={movie?.title || ''}
                  className="w-10 h-14 rounded-lg object-cover border border-slate-800 shadow shrink-0"
                />
              ) : (
                <div className="w-10 h-14 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-center shrink-0 text-slate-500">
                  <Film className="w-5 h-5" />
                </div>
              )}

              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-base sm:text-lg font-bold text-white tracking-tight leading-tight">
                    {loadingMovie ? 'Loading Movie...' : movie?.title}
                  </h1>
                  {movie?.language && (
                    <Badge variant="brand" size="xs">
                      {movie.language}
                    </Badge>
                  )}
                </div>

                <div className="flex items-center gap-2 text-xs text-slate-400 mt-1">
                  {movie?.durationMinutes > 0 && (
                    <span>{formatDuration(movie.durationMinutes)}</span>
                  )}
                  {movie?.genres?.length > 0 && (
                    <>
                      <span className="text-slate-600">•</span>
                      <span>{movie.genres.slice(0, 2).join(', ')}</span>
                    </>
                  )}
                  <span className="text-slate-600">•</span>
                  <span className="text-slate-300 font-medium">{selectedCity}</span>
                </div>
              </div>
            </div>

            <Link to={`/movies/${movieId}`}>
              <Button variant="ghost" size="sm" className="text-xs text-slate-400 hover:text-white">
                View Film Details
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* 2. DATE STRIP & FORMAT FILTERS */}
      <section className="border-b border-slate-800/60 bg-slate-900/30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-5 space-y-4">
          {/* Horizontal Calendar Date Strip */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                Select Screening Date
              </span>
              <span className="text-xs text-slate-500">
                {theatreGroupedShows.length} Theatre{theatreGroupedShows.length !== 1 ? 's' : ''} with active shows
              </span>
            </div>
            <DateSelector
              selectedDate={selectedDate}
              onSelectDate={(newDate) => setSelectedDate(newDate)}
              daysCount={7}
            />
          </div>

          {/* Format Chips & Filter Strip */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800/40">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-400 mr-1">
                Format:
              </span>
              {['ALL', '2D', '3D', 'IMAX'].map((fmt) => (
                <Chip
                  key={fmt}
                  label={fmt}
                  size="sm"
                  active={selectedFormat === fmt}
                  onClick={() => setSelectedFormat(fmt)}
                />
              ))}
            </div>

            <div className="flex items-center gap-4 text-xs text-slate-400">
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span>Free Cancellation</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-red-500" />
                <span>Instant Seat Lock</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. SHOWTIME & THEATRE LISTINGS */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        <div className="flex items-center justify-between pb-2 border-b border-slate-800/80">
          <div>
            <h2 className="text-lg font-bold text-white tracking-tight flex items-center gap-2 font-heading">
              <span>Theatres in {selectedCity}</span>
              <Badge variant="default" size="xs">
                {selectedDate}
              </Badge>
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Click any showtime to pick seats with live inventory locking.
            </p>
          </div>

          <Button
            variant="ghost"
            size="sm"
            onClick={() => refetchShows()}
            leftIcon={<RefreshCw className="w-3.5 h-3.5 text-slate-400" />}
            className="text-xs text-slate-400"
          >
            Refresh
          </Button>
        </div>

        {/* Loading State */}
        {(loadingShows || loadingTheatres) && (
          <div className="space-y-4">
            <ShowtimeGroupSkeleton />
            <ShowtimeGroupSkeleton />
            <ShowtimeGroupSkeleton />
          </div>
        )}

        {/* Error State */}
        {showsError && !loadingShows && (
          <ErrorState
            title="Failed to Load Shows"
            message={showsError.message || 'Unable to retrieve showtimes from BookNGo Show Service.'}
            onRetry={refetchShows}
          />
        )}

        {/* Empty State */}
        {!loadingShows && !showsError && theatreGroupedShows.length === 0 && (
          <EmptyState
            title="No Shows Available"
            description={`There are no scheduled screenings for ${movie?.title || 'this movie'} in ${selectedCity} on ${selectedDate}. Try selecting another date above.`}
            icon={<Clock className="w-8 h-8 text-slate-500" />}
          >
            <div className="pt-2">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setSelectedDate(getTodayIsoString())}
              >
                Reset to Today
              </Button>
            </div>
          </EmptyState>
        )}

        {/* Theatres & Showtimes Groupings */}
        {!loadingShows &&
          !showsError &&
          theatreGroupedShows.map(({ theatre, shows: theatreShows }) => (
            <div
              key={theatre.theatreId}
              className="p-5 sm:p-6 bg-slate-900/80 border border-slate-800/90 rounded-2xl space-y-4 shadow-lg hover:border-slate-700/80 transition-colors"
            >
              {/* Theatre Header */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-800/60">
                <div>
                  <h3 className="text-base sm:text-lg font-bold text-white tracking-tight flex items-center gap-2">
                    <span>{theatre.name}</span>
                  </h3>
                  <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-0.5">
                    <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0" />
                    <span>{theatre.address || theatre.city || selectedCity}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <Badge variant="default" size="xs">
                    M-Ticket Available
                  </Badge>
                  <Badge variant="success" size="xs">
                    Food &amp; Beverage
                  </Badge>
                </div>
              </div>

              {/* Showtimes Pills */}
              <div className="flex flex-wrap items-center gap-3 pt-1">
                {theatreShows.map((show) => {
                  const showTime = formatShowTime(show.startsAt);
                  const isScheduled = show.status === 'SCHEDULED';

                  return (
                    <ShowtimeButton
                      key={show.showId}
                      time={showTime}
                      format={show.showFormat || '2D'}
                      cancellable={true}
                      disabled={!isScheduled}
                      onClick={() => navigate(`/shows/${show.showId}/seats`)}
                    />
                  );
                })}
              </div>
            </div>
          ))}
      </main>
    </div>
  );
}

export default ShowDiscoveryPage;
