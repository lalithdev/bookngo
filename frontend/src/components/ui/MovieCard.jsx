import { Link } from 'react-router';
import { MoviePoster } from './MoviePoster';
import { Button } from './Button';

function formatDuration(minutes) {
  if (!minutes || minutes <= 0) return '';
  const hrs = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hrs > 0 && mins > 0) return `${hrs}h ${mins}m`;
  if (hrs > 0) return `${hrs}h`;
  return `${mins}m`;
}

export function MovieCard({
  movie,
  showBookButton = true,
  className = '',
}) {
  if (!movie) return null;

  const {
    movieId,
    title,
    language,
    genres = [],
    durationMinutes,
  } = movie;

  const formattedGenres = Array.isArray(genres)
    ? genres.slice(0, 3).join(' • ')
    : genres;

  return (
    <div
      className={`group relative bg-slate-900/90 border border-slate-800/90 hover:border-red-600/50 rounded-2xl overflow-hidden shadow-xl shadow-black/50 hover:shadow-2xl hover:shadow-red-950/20 transition-all duration-300 flex flex-col h-full ${className}`}
    >
      {/* Poster */}
      <Link
        to={`/movies/${movieId}`}
        className="block relative aspect-[2/3] w-full overflow-hidden select-none"
        aria-label={`View details for ${title}`}
      >
        <MoviePoster movie={movie} />

        {/* Hover Quick Action Overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/40 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex flex-col justify-end p-4">
          <Button
            variant="primary"
            size="sm"
            className="w-full text-xs font-bold py-2 shadow-xl shadow-red-600/30"
          >
            Book Tickets
          </Button>
        </div>
      </Link>

      {/* Movie Details */}
      <div className="p-4 flex-1 flex flex-col justify-between space-y-2 bg-slate-900/90">
        <div>
          {formattedGenres && (
            <div className="text-[11px] font-medium text-slate-400 truncate mb-1">
              {formattedGenres}
            </div>
          )}

          <Link
            to={`/movies/${movieId}`}
            className="text-sm sm:text-base font-bold text-white hover:text-red-400 transition-colors line-clamp-1 block tracking-tight"
            title={title}
          >
            {title}
          </Link>

          <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-1">
            {language && <span className="font-semibold text-slate-300">{language}</span>}
            {language && durationMinutes > 0 && <span className="text-slate-600">•</span>}
            {durationMinutes > 0 && <span>{formatDuration(durationMinutes)}</span>}
          </div>
        </div>

        {showBookButton && (
          <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between gap-2">
            <span className="text-[11px] font-medium text-slate-400">
              Now Showing
            </span>

            <Link to={`/movies/${movieId}`}>
              <Button
                variant="primary"
                size="sm"
                className="text-xs px-3.5 py-1 font-bold rounded-lg shadow-md shadow-red-600/20"
              >
                Book
              </Button>
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}

export default MovieCard;
