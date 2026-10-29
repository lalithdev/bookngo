import { useQuery } from '@tanstack/react-query';
import { fetchMovies } from '../api/moviesApi';
import { queryKeys } from '../../../app/queryClient';

export function useMovies(filters = {}) {
  return useQuery({
    queryKey: queryKeys.movies.list(filters),
    queryFn: ({ signal }) => fetchMovies({ ...filters, signal }),
  });
}
