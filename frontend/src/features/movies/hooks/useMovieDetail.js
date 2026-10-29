import { useQuery } from '@tanstack/react-query';
import { fetchMovieById } from '../api/moviesApi';
import { queryKeys } from '../../../app/queryClient';

export function useMovieDetail(movieId) {
  return useQuery({
    queryKey: queryKeys.movies.detail(movieId),
    queryFn: ({ signal }) => fetchMovieById(movieId, { signal }),
    enabled: Boolean(movieId),
  });
}
