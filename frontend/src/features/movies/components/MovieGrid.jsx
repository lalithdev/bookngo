import { MovieCard } from './MovieCard';
import { MovieCardSkeleton } from '../../../components/ui/Skeleton';
import { EmptyState } from '../../../components/ui/EmptyState';
import { ErrorState } from '../../../components/ui/ErrorState';

export function MovieGrid({
  movies = [],
  isLoading = false,
  error = null,
  onRetry,
  emptyTitle = 'No movies found',
  emptyDescription = 'There are no active movies currently available matching your criteria.',
}) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {Array.from({ length: 8 }).map((_, i) => (
          <MovieCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <ErrorState
        title="Failed to Load Movies"
        message={error.message || 'Unable to retrieve movies catalog from BookNGo services.'}
        onRetry={onRetry}
      />
    );
  }

  if (!movies || movies.length === 0) {
    return (
      <EmptyState
        title={emptyTitle}
        description={emptyDescription}
      />
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      {movies.map((movie) => (
        <MovieCard key={movie.movieId} movie={movie} />
      ))}
    </div>
  );
}
