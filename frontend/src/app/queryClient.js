import { QueryClient } from '@tanstack/react-query';

export const queryKeys = {
  movies: {
    all: ['movies'],
    list: (filters = {}) => ['movies', 'list', filters],
    detail: (movieId) => ['movies', 'detail', movieId],
  },
  auth: {
    me: ['auth', 'me'],
  },
  // Future domain query key foundations
  theatres: {
    all: ['theatres'],
    list: (city) => ['theatres', 'list', { city }],
  },
  shows: {
    list: (filters = {}) => ['shows', 'list', filters],
    detail: (showId) => ['shows', 'detail', showId],
  },
  bookings: {
    seatInventory: (showId) => ['shows', showId, 'seat-inventory'],
    myList: () => ['bookings', 'me'],
    detail: (bookingId) => ['bookings', 'detail', bookingId],
  },
};

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 2, // 2 minutes
      gcTime: 1000 * 60 * 10,   // 10 minutes
      retry: (failureCount, error) => {
        // Do not retry 4xx errors
        if (error?.status >= 400 && error?.status < 500) {
          return false;
        }
        return failureCount < 2;
      },
      refetchOnWindowFocus: false,
    },
  },
});
