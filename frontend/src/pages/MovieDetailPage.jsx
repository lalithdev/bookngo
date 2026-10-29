import { useState } from 'react';
import { useParams, Link } from 'react-router';
import {
  Clock,
  Calendar,
  Film,
  Ticket,
  ChevronRight,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import { useMovieDetail } from '../features/movies/hooks/useMovieDetail';
import { getMoviePosterUrl, getMovieCoverUrl } from '../features/movies/lib/moviePosters';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { ErrorState } from '../components/ui/ErrorState';
import { Skeleton } from '../components/ui/Skeleton';

function formatDuration(minutes) {
  if (!minutes || minutes <= 0) return '';
  const hrs = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hrs > 0 && mins > 0) return `${hrs}h ${mins}m`;
  if (hrs > 0) return `${hrs}h`;
  return `${mins}m`;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
}

export function MovieDetailPage() {
  const { movieId } = useParams();
  const { data: movie, isLoading, error, refetch } = useMovieDetail(movieId);
  const [imageError, setImageError] = useState(false);

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-pulse">
        <Skeleton className="h-[420px] w-full rounded-3xl" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <Skeleton className="h-64 rounded-2xl" />
          <Skeleton className="h-64 md:col-span-2 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <ErrorState
          title="Movie Details Unavailable"
          message={error.message || 'Unable to retrieve details for this movie title.'}
          onRetry={refetch}
        />
      </div>
    );
  }

  if (!movie) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16">
        <ErrorState
          title="Movie Not Found"
          message="The requested movie record does not exist or is currently inactive."
        />
      </div>
    );
  }

  const {
    title,
    description,
    language,
    genres = [],
    durationMinutes,
    releaseDate,
    metadata = {},
  } = movie;

  const cast = Array.isArray(metadata?.cast) ? metadata.cast : [];
  const director = metadata?.director || null;

  const resolvedPosterUrl = getMoviePosterUrl(movie);
  const resolvedCoverUrl = getMovieCoverUrl(movie);
  const showFallback = imageError || !resolvedPosterUrl;

  return (
    <div className="w-full space-y-0 pb-20">
      {/* Cinematic Backdrop Hero */}
      <section className="relative w-full overflow-hidden bg-[#0B0F17] border-b border-slate-800/80">
        {/* Full-bleed background cover image */}
        {resolvedCoverUrl && (
          <div className="absolute inset-0 z-0 overflow-hidden pointer-events-none select-none">
            <img
              src={resolvedCoverUrl}
              alt=""
              className="w-full h-full object-cover object-[70%_center] scale-105 opacity-25 filter blur-sm"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#0B0F17] via-[#0B0F17]/80 to-[#0B0F17]/40" />
            <div className="absolute inset-0 bg-gradient-to-r from-[#0B0F17] via-transparent to-[#0B0F17]" />
          </div>
        )}

        <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
          {/* Breadcrumb Navigation */}
          <nav className="flex items-center gap-2 text-xs font-medium text-slate-400 mb-8">
            <Link to="/" className="hover:text-white transition-colors">
              Home
            </Link>
            <ChevronRight className="w-3.5 h-3.5 text-slate-600" />
            <Link to="/movies" className="hover:text-white transition-colors">
              Movies
            </Link>
            <ChevronRight className="w-3.5 h-3.5 text-slate-600" />
            <span className="text-white font-semibold truncate max-w-xs sm:max-w-md">
              {title}
            </span>
          </nav>

          <div className="grid grid-cols-1 md:grid-cols-12 gap-8 lg:gap-12 items-start">
            {/* Left: Movie Poster */}
            <div className="md:col-span-5 lg:col-span-4 max-w-[280px] sm:max-w-[320px] mx-auto md:max-w-none w-full">
              <div className="relative aspect-[2/3] w-full rounded-2xl overflow-hidden bg-slate-950 border border-slate-700/60 shadow-2xl shadow-black/80 group">
                {!showFallback ? (
                  <img
                    src={resolvedPosterUrl}
                    alt={title}
                    onError={() => setImageError(true)}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex flex-col justify-between p-6 sm:p-8 text-center bg-gradient-to-br from-slate-950 via-slate-900 to-red-950/40 select-none">
                    <span className="text-xs uppercase font-black tracking-widest text-red-400 bg-red-950/80 px-2.5 py-1 rounded-lg border border-red-500/30 self-start">
                      BookNGo
                    </span>
                    <div className="my-auto space-y-4">
                      <div className="w-16 h-16 mx-auto rounded-2xl bg-red-600/15 border border-red-500/30 flex items-center justify-center text-red-400 shadow-xl shadow-red-600/20">
                        <Film className="w-8 h-8" />
                      </div>
                      <h3 className="text-xl font-bold text-white tracking-tight leading-snug">
                        {title}
                      </h3>
                    </div>
                    <span className="text-xs text-slate-400 font-medium">
                      {language}
                    </span>
                  </div>
                )}
              </div>
            </div>

            {/* Right: Movie Details & Primary Booking CTA */}
            <div className="md:col-span-7 lg:col-span-8 space-y-6">
              {/* Badges & Formats */}
              <div className="flex flex-wrap items-center gap-2">
                {language && (
                  <Badge variant="brand" size="md">
                    {language}
                  </Badge>
                )}
                <Badge variant="default" size="md">
                  2D
                </Badge>
                <Badge variant="default" size="md">
                  3D
                </Badge>
                <Badge variant="imax" size="md">
                  IMAX
                </Badge>
                {genres.map((g) => (
                  <Badge key={g} variant="outline" size="md">
                    {g}
                  </Badge>
                ))}
              </div>

              {/* Title */}
              <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight leading-[1.08] font-heading">
                {title}
              </h1>

              {/* Duration, Release Date */}
              <div className="flex flex-wrap items-center gap-4 sm:gap-6 text-xs sm:text-sm text-slate-300 font-medium pt-1">
                {durationMinutes > 0 && (
                  <div className="flex items-center gap-2">
                    <Clock className="w-4 h-4 text-red-500" />
                    <span>{formatDuration(durationMinutes)}</span>
                  </div>
                )}
                {releaseDate && (
                  <div className="flex items-center gap-2">
                    <Calendar className="w-4 h-4 text-red-500" />
                    <span>{formatDate(releaseDate)}</span>
                  </div>
                )}
                <div className="flex items-center gap-2 text-emerald-400 font-semibold">
                  <ShieldCheck className="w-4 h-4" />
                  <span>Free Cancellation Available</span>
                </div>
              </div>

              {/* Prominent Red Booking CTA Button */}
              <div className="pt-2 flex flex-wrap gap-4">
                <Link to={`/movies/${movieId}/shows`} className="w-full sm:w-auto">
                  <Button
                    variant="primary"
                    size="lg"
                    className="w-full sm:w-auto text-base px-8 py-3.5 shadow-2xl shadow-red-600/40 font-bold flex items-center justify-center gap-2.5 rounded-xl"
                    leftIcon={<Ticket className="w-5 h-5" />}
                  >
                    Book Tickets
                  </Button>
                </Link>
              </div>

              {/* Synopsis Section */}
              <div className="space-y-2 border-t border-slate-800/80 pt-6">
                <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Synopsis
                </h2>
                <p className="text-slate-300 text-sm sm:text-base leading-relaxed max-w-3xl">
                  {description ||
                    'Experience this film in master-grade projection with real-time seat locking and instant gate pass admission.'}
                </p>
              </div>

              {/* Cast & Crew Section */}
              {(director || cast.length > 0) && (
                <div className="space-y-4 border-t border-slate-800/80 pt-6">
                  <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Cast &amp; Crew
                  </h2>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {director && (
                      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4">
                        <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wide">
                          Director
                        </div>
                        <div className="text-sm font-bold text-white mt-1">
                          {director}
                        </div>
                      </div>
                    )}
                    {cast.length > 0 && (
                      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4">
                        <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wide">
                          Starring
                        </div>
                        <div className="text-sm font-bold text-white mt-1">
                          {cast.join(', ')}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Showtime Quick Selection Prompt */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">
        <div className="rounded-2xl bg-gradient-to-r from-red-950/20 via-slate-900 to-slate-900 border border-red-500/20 p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-xl">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-red-500" />
              <h3 className="text-base font-bold text-white">
                Ready to watch {title}?
              </h3>
            </div>
            <p className="text-xs text-slate-400">
              Browse available showtimes across certified theatres in Vijayawada with live inventory synchronization.
            </p>
          </div>

          <Link to={`/movies/${movieId}/shows`} className="shrink-0 w-full sm:w-auto">
            <Button variant="primary" size="md" className="w-full sm:w-auto">
              Select Showtime &amp; Theatres
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}

export default MovieDetailPage;
