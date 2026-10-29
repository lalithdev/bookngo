import { useState } from 'react';
import { Link } from 'react-router';
import { getMoviePosterUrl } from '../lib/moviePosters';
import { Button } from '../../../components/ui/Button';

function formatDuration(minutes) {
  if (!minutes || minutes <= 0) return '';
  const hrs = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hrs > 0 && mins > 0) return `${hrs}h ${mins}m`;
  if (hrs > 0) return `${hrs}h`;
  return `${mins}m`;
}

export function MovieCard({ movie }) {
  const [imageError, setImageError] = useState(false);

  const {
    movieId,
    title,
    language,
    genres = [],
    durationMinutes,
  } = movie;

  const resolvedPosterUrl = getMoviePosterUrl(movie);
  const showFallback = imageError || !resolvedPosterUrl;

  return (
    <div className="group relative bg-[#111622] border border-slate-800/80 hover:border-red-600/50 rounded-2xl overflow-hidden shadow-xl shadow-black/50 hover:shadow-2xl hover:shadow-red-950/20 transition-all duration-300 flex flex-col h-full">
      {/* Poster Container with strict 2:3 aspect ratio */}
      <div className="relative aspect-[2/3] w-full overflow-hidden bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 select-none">
        {!showFallback ? (
          <img
            src={resolvedPosterUrl}
            alt={title}
            onError={() => setImageError(true)}
            loading="lazy"
            className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-500 ease-out"
          />
        ) : (
          /* Intentional BookNGo Cinematic Fallback Poster */
          <div className="w-full h-full relative flex flex-col justify-between p-5 text-center bg-gradient-to-br from-slate-950 via-slate-900 to-red-950/40 overflow-hidden">
            {/* Film icon background watermark */}
            <div className="absolute inset-0 opacity-10 pointer-events-none flex items-center justify-center">
              <svg className="w-44 h-44 text-red-500" fill="currentColor" viewBox="0 0 24 24">
                <path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z" />
              </svg>
            </div>

            {/* Top header */}
            <div className="relative z-10 flex justify-between items-center w-full">
              <span className="text-[10px] uppercase font-black tracking-widest text-red-500 bg-red-950/70 px-2 py-0.5 rounded border border-red-500/20">
                BookNGo
              </span>
              {language && (
                <span className="text-[11px] font-semibold text-slate-300 bg-slate-900/90 px-2 py-0.5 rounded border border-slate-700/60">
                  {language}
                </span>
              )}
            </div>

            {/* Center title & icon */}
            <div className="relative z-10 my-auto space-y-3 px-2">
              <div className="w-12 h-12 mx-auto rounded-xl bg-red-600/15 border border-red-500/30 flex items-center justify-center text-red-400 shadow-lg shadow-red-600/20 group-hover:scale-110 transition-transform duration-300">
                <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                </svg>
              </div>
              <h4 className="text-sm sm:text-base font-bold text-white leading-snug line-clamp-3 tracking-tight">
                {title}
              </h4>
            </div>

            {/* Bottom duration indicator */}
            <div className="relative z-10 text-[11px] font-medium text-slate-400 border-t border-slate-800/80 pt-2">
              {formatDuration(durationMinutes) || 'Feature Film'}
            </div>
          </div>
        )}

        {/* Prototype-style Badges on Poster */}
        {!showFallback && (
          <>
            <div className="absolute top-2.5 left-2.5 pointer-events-none flex items-center gap-1.5">
              <span className="bg-black/60 backdrop-blur-md text-amber-400 text-[10px] font-bold px-2 py-0.5 rounded border border-white/10 flex items-center gap-1 shadow">
                <span>★</span> 8.8/10
              </span>
            </div>
            <div className="absolute top-2.5 right-2.5 pointer-events-none">
              <span className="bg-red-600/90 backdrop-blur-md text-white text-[10px] font-black uppercase px-1.5 py-0.5 rounded shadow">
                2D
              </span>
            </div>
          </>
        )}

        {/* Hover Gradient Overlay with Quick Action Button */}
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-4">
          <Link to={`/movies/${movieId}`} className="w-full">
            <Button variant="primary" size="sm" className="w-full shadow-xl shadow-red-600/30 text-xs font-bold py-2">
              View Details &amp; Shows
            </Button>
          </Link>
        </div>
      </div>

      {/* Card Content & Metadata matching Prototype */}
      <div className="p-3.5 sm:p-4 flex-1 flex flex-col justify-between space-y-2.5 bg-[#111622]/90">
        <div>
          {/* Genre line */}
          <div className="text-[10px] sm:text-[11px] font-medium text-slate-400 mb-0.5 line-clamp-1">
            {genres.length > 0 ? genres.slice(0, 3).join('/') : 'Cinema'}
          </div>

          {/* Title */}
          <Link
            to={`/movies/${movieId}`}
            className="text-sm sm:text-base font-bold text-white hover:text-red-400 transition-colors line-clamp-1 block tracking-tight"
            title={title}
          >
            {title}
          </Link>

          {/* Language & Duration */}
          <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-0.5">
            <span>{language}</span>
            {durationMinutes > 0 && (
              <>
                <span className="text-slate-600">•</span>
                <span>{formatDuration(durationMinutes)}</span>
              </>
            )}
          </div>
        </div>

        {/* Bottom CTA Row */}
        <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between gap-2">
          <span className="text-[11px] font-medium text-slate-400">
            From ₹200
          </span>

          <Link to={`/movies/${movieId}`}>
            <Button variant="primary" size="sm" className="text-xs px-3.5 py-1 font-bold rounded-lg shadow-md shadow-red-600/20">
              Book Now
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}

export default MovieCard;
