/**
 * Local Poster & Cover Asset Resolver for BookNGo.
 * Presentation-layer mapping connecting seeded/active movies to real poster and cover artwork.
 * Preserves graceful fallback for unmapped or failed assets.
 */

export const MOVIE_POSTER_MAP = {
  'Avengers: Endgame – Encore': '/posters/avengers-endgame.jpg',
  'Mirzapur: The Movie': '/posters/mirzapur.jpg',
  'Hanuman Ansh': '/posters/hanuman-ansh.jpg',
  'Irumudi': '/posters/irumudi.jpg',
  'Mandaadi': '/posters/mandaadi.jpg',
  'Resident Evil': '/posters/resident-evil.webp',
  'Secret Soldier': '/posters/secret-soldier.jpg',
  'Mahendragiri Varahi': '/posters/mahendragiri-varahi.jpg',
  'Happy Journey': '/posters/happy-journey.jpg',
  'Kaagitham Padavalu': '/posters/kaagitham-padavalu.jpg',
  'Pithapuramlo Ala Modalaindi': '/posters/pithapuramlo-ala-modalaindi.jpg',
  'Okka Chuputho': '/posters/okka-chuputho.jpg',
  'Immortal': '/posters/immortal.jpg',
  'Epic - First Semester': '/posters/epic.avif',
  'Bethlehem Kudumba Unit': '/posters/bethlehem-kudumba-unitavif.avif',
};

export const MOVIE_COVER_MAP = {
  'Avengers: Endgame – Encore': '/posters/endgameencore-cover-image.jpg',
  'Mirzapur: The Movie': '/posters/mirzapur-cover-image.jpg',
  'Hanuman Ansh': '/posters/hanumanansh-cover-image.jpg',
  'Irumudi': '/posters/irumudi-cover-image.avif',
  'Mandaadi': '/posters/mandaadi-cover-image.jpg',
  'Mahendragiri Varahi': '/posters/mahendragiri-varahi--cover-image.avif',
  'Bethlehem Kudumba Unit': '/posters/Bethlehem Kudumba Unit-cover-image.jpg',
  'Epic - First Semester': '/posters/epic-cover-imageavif.avif',
};

/**
 * Resolves the vertical 2:3 portrait poster artwork URL.
 */
export function getMoviePosterUrl(movie) {
  if (!movie) return null;

  if (movie.title && MOVIE_POSTER_MAP[movie.title]) {
    return MOVIE_POSTER_MAP[movie.title];
  }

  if (
    movie.posterReference &&
    (movie.posterReference.startsWith('http://') ||
      movie.posterReference.startsWith('https://') ||
      movie.posterReference.startsWith('/'))
  ) {
    return movie.posterReference;
  }

  return null;
}

/**
 * Resolves the wide cinematic cover/backdrop artwork URL for hero display.
 * Uses <movie>-cover-image.* when available, otherwise falls back to poster.
 */
export function getMovieCoverUrl(movie) {
  if (!movie) return null;

  if (movie.title && MOVIE_COVER_MAP[movie.title]) {
    return MOVIE_COVER_MAP[movie.title];
  }

  return getMoviePosterUrl(movie);
}
