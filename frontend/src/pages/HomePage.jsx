import { useState, useMemo, useEffect } from 'react';
import { Link, useOutletContext } from 'react-router';
import { useMovies } from '../features/movies/hooks/useMovies';
import { MovieGrid } from '../features/movies/components/MovieGrid';
import { RealTimeAvailability } from '../features/shows/components/RealTimeAvailability';
import { getMoviePosterUrl, getMovieCoverUrl } from '../features/movies/lib/moviePosters';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import { Ticket, Film, Clock } from 'lucide-react';

function formatDuration(minutes) {
  if (!minutes || minutes <= 0) return '';
  const hrs = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hrs > 0 && mins > 0) return `${hrs}h ${mins}m`;
  if (hrs > 0) return `${hrs}h`;
  return `${mins}m`;
}

const ACTIVE_MOVIES_FILTER = { status: 'ACTIVE' };
const COMING_SOON_FILTER = { status: 'COMING_SOON' };

export function HomePage() {
  const { selectedCity = 'Vijayawada' } = useOutletContext() || {};
  const { data: movies = [], isLoading, error, refetch } = useMovies(ACTIVE_MOVIES_FILTER);
  const { data: comingSoonMovies = [], isLoading: loadingComingSoon } = useMovies(COMING_SOON_FILTER);

  const [selectedLanguage, setSelectedLanguage] = useState('ALL');
  const [selectedGenre, setSelectedGenre] = useState('ALL');

  // Hero Rotation State
  const [heroIndex, setHeroIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [heroPosterErrorId, setHeroPosterErrorId] = useState(null);
  const [heroCoverErrorId, setHeroCoverErrorId] = useState(null);

  // Rotating movie list: Select top active movies (up to 5) for hero rotation
  const rotatingMovies = useMemo(() => {
    if (!movies || movies.length === 0) return [];
    return movies.slice(0, Math.min(5, movies.length));
  }, [movies]);

  // Current featured movie
  const currentHeroMovie = useMemo(() => {
    if (rotatingMovies.length === 0) return null;
    return rotatingMovies[heroIndex % rotatingMovies.length];
  }, [rotatingMovies, heroIndex]);

  // Automatic Hero Rotation Timer (Every 7 seconds)
  useEffect(() => {
    if (rotatingMovies.length <= 1 || isPaused) return;

    const timer = setInterval(() => {
      setHeroIndex((prev) => (prev + 1) % rotatingMovies.length);
    }, 7000);

    return () => clearInterval(timer);
  }, [rotatingMovies.length, isPaused]);

  // Extract available unique languages and genres dynamically from live data
  const availableLanguages = useMemo(() => {
    if (!movies) return [];
    const set = new Set(movies.map((m) => m.language).filter(Boolean));
    return Array.from(set);
  }, [movies]);

  const availableGenres = useMemo(() => {
    if (!movies) return [];
    const set = new Set(movies.flatMap((m) => m.genres || []).filter(Boolean));
    return Array.from(set);
  }, [movies]);

  // Filter movies for "Now Showing" section
  const filteredMovies = useMemo(() => {
    if (!movies) return [];
    return movies.filter((movie) => {
      const matchLang =
        selectedLanguage === 'ALL' ||
        (movie.language && movie.language.toLowerCase() === selectedLanguage.toLowerCase());
      const matchGenre =
        selectedGenre === 'ALL' ||
        (Array.isArray(movie.genres) &&
          movie.genres.some((g) => g.toLowerCase() === selectedGenre.toLowerCase()));
      return matchLang && matchGenre;
    });
  }, [movies, selectedLanguage, selectedGenre]);

  const heroPosterUrl = currentHeroMovie ? getMoviePosterUrl(currentHeroMovie) : null;
  const heroCoverUrl = currentHeroMovie ? getMovieCoverUrl(currentHeroMovie) : null;

  const showHeroPosterFallback =
    (currentHeroMovie && heroPosterErrorId === currentHeroMovie.movieId) || !heroPosterUrl;

  const showHeroCover =
    heroCoverUrl && (!currentHeroMovie || heroCoverErrorId !== currentHeroMovie.movieId);

  return (
    <div className="w-full space-y-0 pb-20">
      {/* ============================================================ */}
      {/* 1. FULL-WIDTH CINEMATIC HERO SECTION                         */}
      {/* ============================================================ */}
      {isLoading ? (
        <div className="w-full h-[520px] bg-slate-900/60 animate-pulse flex items-center justify-center">
          <div className="text-slate-500 text-xs font-semibold tracking-wider uppercase">Loading Featured Movies...</div>
        </div>
      ) : currentHeroMovie ? (
        <section
          onMouseEnter={() => setIsPaused(true)}
          onMouseLeave={() => setIsPaused(false)}
          className="relative w-full overflow-hidden bg-[#0B0F17] min-h-[500px] lg:h-[540px] flex items-center border-b border-slate-800/60"
        >
          {/* Cover Image Background (Full-bleed across viewport) */}
          <div className="absolute inset-0 z-0 overflow-hidden pointer-events-none select-none">
            {showHeroCover && (
              <img
                key={`cover-${currentHeroMovie.movieId}`}
                src={heroCoverUrl}
                alt=""
                onError={() => setHeroCoverErrorId(currentHeroMovie.movieId)}
                className="w-full h-full object-cover object-[70%_center] scale-[1.02] opacity-50 sm:opacity-65 transition-opacity duration-700 animate-[hero-fade_800ms_ease-out]"
              />
            )}
            {/* Cinematic Gradient Overlays */}
            <div className="absolute inset-0 bg-gradient-to-r from-[#0B0F17] via-[#0B0F17]/85 sm:via-[#0B0F17]/70 to-[#0B0F17]/20" />
            <div className="absolute inset-x-0 bottom-0 h-32 bg-gradient-to-t from-[#0B0F17] via-[#0B0F17]/60 to-transparent" />
            <div className="absolute inset-x-0 top-0 h-16 bg-gradient-to-b from-[#0B0F17]/60 to-transparent" />
          </div>

          {/* Inner Content Container */}
          <div className="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 lg:py-0 h-full">
            <div className="flex flex-col md:flex-row items-center gap-6 lg:gap-10">
              {/* Left Column: Portrait Movie Poster */}
              <div className="w-[180px] sm:w-[220px] lg:w-[240px] aspect-[2/3] shrink-0 rounded-2xl overflow-hidden shadow-2xl shadow-black/90 border border-white/10 bg-slate-950 group">
                {!showHeroPosterFallback ? (
                  <img
                    key={`poster-${currentHeroMovie.movieId}`}
                    src={heroPosterUrl}
                    alt={currentHeroMovie.title}
                    onError={() => setHeroPosterErrorId(currentHeroMovie.movieId)}
                    className="w-full h-full object-cover object-center transition-transform duration-500 group-hover:scale-105"
                  />
                ) : (
                  <div className="w-full h-full flex flex-col justify-between p-6 text-center bg-gradient-to-br from-slate-950 via-slate-900 to-red-950/40 select-none">
                    <span className="text-[10px] uppercase font-black tracking-widest text-red-400 bg-red-950/80 px-2.5 py-1 rounded-lg border border-red-500/30 self-start">
                      Featured
                    </span>
                    <div className="my-auto space-y-2">
                      <div className="w-14 h-14 mx-auto rounded-2xl bg-red-600/15 border border-red-500/30 flex items-center justify-center text-red-400 shadow-md shadow-red-600/20">
                        <Film className="w-7 h-7" />
                      </div>
                      <h4 className="text-base font-bold text-white leading-snug line-clamp-2">
                        {currentHeroMovie.title}
                      </h4>
                    </div>
                    <span className="text-xs text-slate-400 font-medium">
                      {currentHeroMovie.language}
                    </span>
                  </div>
                )}
              </div>

              {/* Right Column: Movie Details */}
              <div className="flex-1 max-w-[720px] space-y-3.5 text-center md:text-left">
                {/* Location Availability Badge */}
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-600/10 border border-red-500/30 text-xs font-bold tracking-wider text-red-400">
                  <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
                  <span>NOW SHOWING IN {selectedCity.toUpperCase()}</span>
                </div>

                {/* Movie Title */}
                <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight leading-[1.08] font-heading">
                  {currentHeroMovie.title}
                </h1>

                {/* Badges / Formats */}
                <div className="flex flex-wrap items-center justify-center md:justify-start gap-2 pt-0.5">
                  <span className="bg-slate-900/90 border border-slate-700/80 text-slate-200 text-xs font-semibold px-2.5 py-0.5 rounded-lg">
                    2D
                  </span>
                  <span className="bg-slate-900/90 border border-slate-700/80 text-slate-200 text-xs font-semibold px-2.5 py-0.5 rounded-lg">
                    3D
                  </span>
                  <span className="bg-blue-950/60 border border-blue-500/40 text-blue-300 text-xs font-black tracking-wider px-2.5 py-0.5 rounded-lg">
                    IMAX
                  </span>
                  {currentHeroMovie.language && (
                    <span className="bg-red-950/70 border border-red-500/40 text-red-300 text-xs font-bold px-2.5 py-0.5 rounded-lg">
                      {currentHeroMovie.language}
                    </span>
                  )}
                </div>

                {/* Duration & Genres */}
                <div className="flex flex-wrap items-center justify-center md:justify-start gap-2.5 text-xs sm:text-sm text-slate-300 font-medium">
                  {currentHeroMovie.durationMinutes > 0 && (
                    <span className="flex items-center gap-1.5 text-slate-300">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      {formatDuration(currentHeroMovie.durationMinutes)}
                    </span>
                  )}
                  {currentHeroMovie.genres && currentHeroMovie.genres.length > 0 && (
                    <>
                      <span className="text-slate-600">•</span>
                      <span>{currentHeroMovie.genres.join(', ')}</span>
                    </>
                  )}
                  {currentHeroMovie.releaseDate && (
                    <>
                      <span className="text-slate-600">•</span>
                      <span>
                        {new Date(currentHeroMovie.releaseDate).toLocaleDateString('en-IN', {
                          day: 'numeric',
                          month: 'short',
                          year: 'numeric',
                        })}
                      </span>
                    </>
                  )}
                </div>

                {/* Synopsis */}
                <p className="text-slate-300 text-xs sm:text-sm leading-relaxed max-w-[620px] line-clamp-2 mx-auto md:mx-0">
                  {currentHeroMovie.description ||
                    'Experience unmatched cinematic immersion across premier auditoriums in Vijayawada with instantaneous seat locking.'}
                </p>

                {/* CTAs */}
                <div className="flex flex-wrap items-center justify-center md:justify-start gap-3.5 pt-2">
                  <Link to={`/movies/${currentHeroMovie.movieId}`}>
                    <Button
                      variant="primary"
                      size="lg"
                      className="font-bold px-7 rounded-xl flex items-center gap-2 text-sm shadow-xl shadow-red-600/30"
                      leftIcon={<Ticket className="w-4 h-4" />}
                    >
                      Book Tickets
                    </Button>
                  </Link>

                  <Link to="/movies">
                    <Button
                      variant="secondary"
                      size="lg"
                      className="px-6 rounded-xl font-semibold text-sm bg-slate-900/80 hover:bg-slate-800 text-slate-200 border border-slate-700/80"
                    >
                      Browse All Movies
                    </Button>
                  </Link>
                </div>

                {/* Slide Rotation Indicators */}
                {rotatingMovies.length > 1 && (
                  <div className="pt-2 flex items-center justify-center md:justify-start gap-2">
                    {rotatingMovies.map((m, idx) => (
                      <button
                        key={m.movieId}
                        onClick={() => setHeroIndex(idx)}
                        className={`h-1.5 rounded-full transition-all duration-300 cursor-pointer ${
                          idx === heroIndex % rotatingMovies.length
                            ? 'w-8 bg-red-600'
                            : 'w-2 bg-slate-700 hover:bg-slate-500'
                        }`}
                        aria-label={`Slide ${idx + 1}`}
                      />
                    ))}
                    <span className="text-[10px] text-slate-500 ml-2 font-mono">
                      {isPaused ? 'Paused' : 'Auto'}
                    </span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </section>
      ) : null}

      {/* Main Content Area (Wide Container) */}
      <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-16">
        {/* ============================================================ */}
        {/* 2. COMPACT FILTER STRIP (NOT A BOXED SAAS CARD)              */}
        {/* ============================================================ */}
        <section className="border-b border-slate-800/80 pb-6 space-y-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            {/* Language Pills */}
            <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0 scrollbar-none">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400 mr-2 whitespace-nowrap">
                Languages:
              </span>
              <button
                onClick={() => setSelectedLanguage('ALL')}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer whitespace-nowrap ${
                  selectedLanguage === 'ALL'
                    ? 'bg-red-600 text-white shadow-md shadow-red-600/30'
                    : 'bg-[#111622] text-slate-300 hover:text-white hover:bg-slate-800 border border-slate-800'
                }`}
              >
                All
              </button>
              {availableLanguages.map((lang) => (
                <button
                  key={lang}
                  onClick={() => setSelectedLanguage(lang)}
                  className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer whitespace-nowrap ${
                    selectedLanguage.toLowerCase() === lang.toLowerCase()
                      ? 'bg-red-600 text-white shadow-md shadow-red-600/30'
                      : 'bg-[#111622] text-slate-300 hover:text-white hover:bg-slate-800 border border-slate-800'
                  }`}
                >
                  {lang}
                </button>
              ))}
            </div>

            {/* Genre Pills */}
            <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0 scrollbar-none">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400 mr-2 whitespace-nowrap">
                Genres:
              </span>
              <button
                onClick={() => setSelectedGenre('ALL')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer whitespace-nowrap ${
                  selectedGenre === 'ALL'
                    ? 'bg-slate-700 text-white'
                    : 'bg-[#111622] text-slate-400 hover:text-white border border-slate-800'
                }`}
              >
                All
              </button>
              {availableGenres.slice(0, 6).map((genre) => (
                <button
                  key={genre}
                  onClick={() => setSelectedGenre(genre)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer whitespace-nowrap ${
                    selectedGenre.toLowerCase() === genre.toLowerCase()
                      ? 'bg-slate-700 text-white'
                      : 'bg-[#111622] text-slate-400 hover:text-white border border-slate-800'
                  }`}
                >
                  {genre}
                </button>
              ))}
            </div>
          </div>
        </section>

        {/* ============================================================ */}
        {/* 3. NOW SHOWING SECTION (IN THEATRES NOW)                     */}
        {/* ============================================================ */}
        <section className="space-y-6">
          <div className="flex items-end justify-between">
            <div>
              <span className="text-[11px] font-bold uppercase tracking-widest text-slate-400 block mb-1">
                In Theatres Now
              </span>
              <div className="flex items-center gap-3">
                <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
                  Now Showing in {selectedCity}
                </h2>
                {!isLoading && (
                  <span className="text-xs font-bold bg-slate-800 text-slate-300 px-2.5 py-0.5 rounded-full border border-slate-700">
                    {filteredMovies.length} Movies
                  </span>
                )}
              </div>
            </div>

            <Link
              to="/movies"
              className="text-xs sm:text-sm font-semibold text-slate-300 hover:text-red-400 transition flex items-center gap-1.5 group"
            >
              <span>Explore All Movies</span>
              <span className="group-hover:translate-x-1 transition-transform">→</span>
            </Link>
          </div>

          <MovieGrid
            movies={filteredMovies}
            isLoading={isLoading}
            error={error}
            onRetry={refetch}
          />
        </section>

        {/* ============================================================ */}
        {/* 4. ULTIMATE IMMERSION / EXPERIENCE SECTION                   */}
        {/* ============================================================ */}
        <section className="space-y-8 pt-4">
          <div className="text-center max-w-xl mx-auto space-y-2">
            <span className="text-xs font-bold uppercase tracking-widest text-red-500">
              Ultimate Immersion
            </span>
            <h3 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
              Experience Cinema Like Never Before
            </h3>
            <p className="text-xs sm:text-sm text-slate-400">
              Choose from state-of-the-art booking protections designed for absolute realism and contention-free reservation.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-[#111622] border border-slate-800/80 hover:border-red-500/40 rounded-2xl p-6 sm:p-7 space-y-4 transition group">
              <div className="w-10 h-10 rounded-xl bg-red-600/15 border border-red-500/30 flex items-center justify-center text-red-400">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h4 className="text-base font-bold text-white tracking-tight uppercase">
                5-Minute Seat Hold
              </h4>
              <p className="text-xs text-slate-400 leading-relaxed">
                Selected seats are held server-side during checkout and automatically released when the hold expires to protect fair queueing.
              </p>
              <div className="text-xs font-semibold text-red-400 group-hover:text-red-300 pt-1 flex items-center gap-1">
                <span>Server-Side Protection</span>
                <span>→</span>
              </div>
            </div>

            <div className="bg-[#111622] border border-slate-800/80 hover:border-amber-500/40 rounded-2xl p-6 sm:p-7 space-y-4 transition group">
              <div className="w-10 h-10 rounded-xl bg-amber-600/15 border border-amber-500/30 flex items-center justify-center text-amber-400">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
              <h4 className="text-base font-bold text-white tracking-tight uppercase">
                Zero Double Booking
              </h4>
              <p className="text-xs text-slate-400 leading-relaxed">
                Transactional seat allocation protects show seats during concurrent booking attempts even during high-demand blockbusters.
              </p>
              <div className="text-xs font-semibold text-amber-400 group-hover:text-amber-300 pt-1 flex items-center gap-1">
                <span>Concurrency Engine</span>
                <span>→</span>
              </div>
            </div>

            <div className="bg-[#111622] border border-slate-800/80 hover:border-emerald-500/40 rounded-2xl p-6 sm:p-7 space-y-4 transition group">
              <div className="w-10 h-10 rounded-xl bg-emerald-600/15 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
                </svg>
              </div>
              <h4 className="text-base font-bold text-white tracking-tight uppercase">
                Digital Ticket
              </h4>
              <p className="text-xs text-slate-400 leading-relaxed">
                Confirmed bookings provide an instant verified digital ticket and QR pass for the customer directly on their device.
              </p>
              <div className="text-xs font-semibold text-emerald-400 group-hover:text-emerald-300 pt-1 flex items-center gap-1">
                <span>Instant Access</span>
                <span>→</span>
              </div>
            </div>
          </div>
        </section>

        {/* ============================================================ */}
        {/* 5. REAL-TIME AVAILABILITY (THEATRES & SHOWS)                 */}
        {/* ============================================================ */}
        <div id="availability">
          <RealTimeAvailability selectedCity={selectedCity} />
        </div>

        {/* ============================================================ */}
        {/* 6. COMING SOON SECTION (TRUTHFUL ARCHITECTURE)               */}
        {/* ============================================================ */}
        <section className="space-y-4 pt-4 border-t border-slate-800/80">
          <div className="flex items-center justify-between">
            <div>
              <span className="text-[11px] font-bold uppercase tracking-widest text-slate-400 block mb-1">
                Coming Soon
              </span>
              <h3 className="text-xl sm:text-2xl font-bold tracking-tight text-white">
                Advance Booking Open
              </h3>
            </div>
          </div>

          {loadingComingSoon ? (
            <Skeleton className="h-32 w-full rounded-2xl" />
          ) : comingSoonMovies.length > 0 ? (
            <MovieGrid movies={comingSoonMovies} />
          ) : (
            <div className="p-8 rounded-2xl bg-[#111622]/60 border border-slate-800/80 text-center space-y-2">
              <div className="w-10 h-10 mx-auto rounded-xl bg-slate-800 flex items-center justify-center text-slate-400">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
              <h4 className="text-sm font-semibold text-slate-200">
                Upcoming titles will appear here when advance booking opens.
              </h4>
              <p className="text-xs text-slate-400 max-w-md mx-auto">
                Screening dates and advance bookings will be published once distributor schedules are officially verified.
              </p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

export default HomePage;
