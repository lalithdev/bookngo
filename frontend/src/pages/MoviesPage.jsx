import { useState, useMemo } from 'react';
import { useSearchParams } from 'react-router';
import { useMovies } from '../features/movies/hooks/useMovies';
import { MovieGrid } from '../features/movies/components/MovieGrid';
import { Button } from '../components/ui/Button';

const COMMON_GENRES = ['Action', 'Drama', 'Thriller', 'Sci-Fi', 'Comedy', 'Romance', 'Animation', 'Crime', 'Family', 'Horror'];
const COMMON_LANGUAGES = ['Telugu', 'Hindi', 'English', 'Tamil', 'Kannada', 'Malayalam'];

export function MoviesPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const titleQuery = searchParams.get('title') || '';
  const [searchInput, setSearchInput] = useState(titleQuery);
  const [selectedLanguage, setSelectedLanguage] = useState('');
  const [selectedGenre, setSelectedGenre] = useState('');

  // Active query parameters passed to Movie Service
  const filters = useMemo(() => ({
    status: 'ACTIVE',
    title: titleQuery || undefined,
    language: selectedLanguage || undefined,
    genre: selectedGenre || undefined,
  }), [titleQuery, selectedLanguage, selectedGenre]);

  const { data: movies, isLoading, error, refetch } = useMovies(filters);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    const nextParams = new URLSearchParams(searchParams);
    if (searchInput.trim()) {
      nextParams.set('title', searchInput.trim());
    } else {
      nextParams.delete('title');
    }
    setSearchParams(nextParams);
  };

  const clearAllFilters = () => {
    setSearchInput('');
    setSelectedLanguage('');
    setSelectedGenre('');
    setSearchParams({});
  };

  const hasActiveFilters = Boolean(titleQuery || selectedLanguage || selectedGenre);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 md:py-8 space-y-8 pb-12">
      {/* Header and Search */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 pb-6 border-b border-slate-800/80">
        <div className="space-y-1">
          <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-white">
            Explore Movies
          </h1>
          <p className="text-xs sm:text-sm text-slate-400">
            Browse currently active verified films playing in theatres
          </p>
        </div>

        <form onSubmit={handleSearchSubmit} className="flex gap-2 max-w-sm w-full">
          <input
            type="text"
            placeholder="Search by title..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="flex-1 bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-red-500 focus:border-red-500 transition-colors"
          />
          <Button type="submit" variant="primary" size="sm" className="px-5 font-semibold">
            Search
          </Button>
        </form>
      </div>

      {/* Filter Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-900/60 p-4 sm:p-5 rounded-2xl border border-slate-800/80">
        <div className="flex flex-wrap items-center gap-4">
          {/* Language Selector */}
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wide">
              Language:
            </span>
            <select
              value={selectedLanguage}
              onChange={(e) => setSelectedLanguage(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:ring-1 focus:ring-red-500 cursor-pointer"
            >
              <option value="">All Languages</option>
              {COMMON_LANGUAGES.map((lang) => (
                <option key={lang} value={lang}>
                  {lang}
                </option>
              ))}
            </select>
          </div>

          {/* Genre Selector */}
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wide">
              Genre:
            </span>
            <select
              value={selectedGenre}
              onChange={(e) => setSelectedGenre(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:ring-1 focus:ring-red-500 cursor-pointer"
            >
              <option value="">All Genres</option>
              {COMMON_GENRES.map((g) => (
                <option key={g} value={g}>
                  {g}
                </option>
              ))}
            </select>
          </div>
        </div>

        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={clearAllFilters} className="text-red-400 hover:text-red-300 text-xs font-semibold">
            Clear All Filters
          </Button>
        )}
      </div>

      {/* Movies Grid */}
      <MovieGrid
        movies={movies}
        isLoading={isLoading}
        error={error}
        onRetry={refetch}
        emptyTitle={hasActiveFilters ? 'No Matching Movies Found' : 'No Active Movies Available'}
        emptyDescription={
          hasActiveFilters
            ? 'We could not find any active films matching your filter criteria. Try clearing search filters.'
            : 'There are currently no active movies scheduled in the BookNGo catalog.'
        }
      />
    </div>
  );
}

export default MoviesPage;
