import { useState } from 'react';
import { Film } from 'lucide-react';
import { getMoviePosterUrl } from '../../features/movies/lib/moviePosters';
import { Badge } from './Badge';

export function MoviePoster({
  movie,
  src = null,
  alt = '',
  aspectRatio = 'aspect-[2/3]',
  certificate = null,
  language = null,
  className = '',
  hoverZoom = true,
}) {
  const [imageError, setImageError] = useState(false);
  const resolvedSrc = src || getMoviePosterUrl(movie);

  const displayAlt = alt || movie?.title || 'Movie Poster';
  const cert = certificate || movie?.certification || movie?.censorRating;
  const lang = language || movie?.language;

  return (
    <div
      className={`relative ${aspectRatio} rounded-2xl overflow-hidden bg-slate-900 border border-slate-800/80 group ${
        hoverZoom ? 'shadow-lg shadow-black/60 group-hover:border-slate-700 transition-all duration-300' : ''
      } ${className}`}
    >
      {resolvedSrc && !imageError ? (
        <img
          src={resolvedSrc}
          alt={displayAlt}
          onError={() => setImageError(true)}
          className={`w-full h-full object-cover transition-transform duration-500 ease-out ${
            hoverZoom ? 'group-hover:scale-105' : ''
          }`}
          loading="lazy"
        />
      ) : (
        <div className="w-full h-full flex flex-col items-center justify-center p-4 bg-gradient-to-b from-slate-900 via-slate-950 to-black text-center">
          <Film className="w-10 h-10 text-slate-700 mb-2 group-hover:text-red-500/50 transition-colors" />
          <span className="text-xs font-semibold text-slate-400 line-clamp-2">
            {displayAlt}
          </span>
        </div>
      )}

      {/* Subtle bottom shadow vignette */}
      <div className="absolute inset-0 bg-gradient-to-t from-slate-950/80 via-transparent to-transparent pointer-events-none opacity-60 group-hover:opacity-40 transition-opacity" />

      {/* Top Overlays: Certificate / Language */}
      <div className="absolute top-2.5 left-2.5 right-2.5 flex items-center justify-between pointer-events-none">
        {cert && (
          <Badge
            variant="default"
            size="xs"
            className="bg-slate-950/80 backdrop-blur-md border-slate-700/80 text-[10px] font-bold"
          >
            {cert}
          </Badge>
        )}
        {lang && (
          <Badge
            variant="default"
            size="xs"
            className="bg-slate-950/80 backdrop-blur-md border-slate-700/80 text-[10px] uppercase font-bold ml-auto"
          >
            {lang}
          </Badge>
        )}
      </div>
    </div>
  );
}

export default MoviePoster;
