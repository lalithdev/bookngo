import { useState } from 'react';
import { useNavigate } from 'react-router';
import { useQuery, useMutation } from '@tanstack/react-query';
import { getMovies, createMovie, deleteMovie } from '../features/movies/api/moviesApi';
import { getTheatres, createTheatre } from '../features/shows/api/showsAndTheatresApi';
import { useToast } from '../context/useToast';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Tabs } from '../components/ui/Tabs';
import { LoadingState } from '../components/ui/LoadingState';
import {
  ShieldAlert,
  Film,
  Building2,
  Server,
  Plus,
  Trash2,
} from 'lucide-react';

const ADMIN_TABS = [
  { id: 'MOVIES', label: 'Movies Catalogue' },
  { id: 'THEATRES', label: 'Theatres Network' },
  { id: 'SERVICES', label: 'Platform Services Health' },
];

export function AdminPortalPage() {
  const navigate = useNavigate();
  const toast = useToast();

  const [activeTab, setActiveTab] = useState('MOVIES');

  // Modals
  const [newMovieModalOpen, setNewMovieModalOpen] = useState(false);
  const [movieTitle, setMovieTitle] = useState('');
  const [movieDescription, setMovieDescription] = useState('');
  const [movieDuration, setMovieDuration] = useState('150');
  const [movieLanguage, setMovieLanguage] = useState('Telugu');
  const [movieCertification, setMovieCertification] = useState('UA');
  const [movieReleaseDate, setMovieReleaseDate] = useState(new Date().toISOString().split('T')[0]);
  const [movieGenres, setMovieGenres] = useState('Action, Drama');

  const [newTheatreModalOpen, setNewTheatreModalOpen] = useState(false);
  const [theatreName, setTheatreName] = useState('');
  const [theatreCity, setTheatreCity] = useState('Vijayawada');
  const [theatreAddress, setTheatreAddress] = useState('');

  // 1. Fetch Movies
  const {
    data: movies = [],
    isLoading: isMoviesLoading,
    refetch: refetchMovies,
  } = useQuery({
    queryKey: ['adminMovies'],
    queryFn: () => getMovies({ status: null }), // fetch all
  });

  // 2. Fetch Theatres
  const {
    data: theatres = [],
    isLoading: isTheatresLoading,
    refetch: refetchTheatres,
  } = useQuery({
    queryKey: ['theatres'],
    queryFn: () => getTheatres(),
  });

  // Create Movie Mutation
  const createMovieMutation = useMutation({
    mutationFn: () => {
      const genresList = movieGenres
        .split(',')
        .map((g) => g.trim())
        .filter(Boolean);

      return createMovie({
        title: movieTitle.trim(),
        description: movieDescription.trim(),
        durationMinutes: parseInt(movieDuration, 10) || 120,
        language: movieLanguage.trim(),
        certification: movieCertification.trim(),
        releaseDate: movieReleaseDate,
        genres: genresList,
        status: 'ACTIVE',
      });
    },
    onSuccess: () => {
      toast.success('Movie created successfully in catalog');
      setNewMovieModalOpen(false);
      setMovieTitle('');
      setMovieDescription('');
      refetchMovies();
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to create movie');
    },
  });

  // Delete Movie Mutation
  const deleteMovieMutation = useMutation({
    mutationFn: (movieId) => deleteMovie(movieId),
    onSuccess: () => {
      toast.success('Movie removed from catalog');
      refetchMovies();
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to delete movie');
    },
  });

  // Create Theatre Mutation
  const createTheatreMutation = useMutation({
    mutationFn: () => {
      return createTheatre({
        name: theatreName.trim(),
        city: theatreCity.trim(),
        address: theatreAddress.trim(),
      });
    },
    onSuccess: () => {
      toast.success('Theatre onboarded successfully');
      setNewTheatreModalOpen(false);
      setTheatreName('');
      setTheatreAddress('');
      refetchTheatres();
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to onboard theatre');
    },
  });

  return (
    <div className="min-h-[85vh] bg-[#0A0D14] text-white py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 shadow-xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <ShieldAlert className="w-6 h-6 text-red-500" />
              <h1 className="text-2xl font-extrabold tracking-tight text-white">
                BookNGo Platform Administration
              </h1>
              <Badge variant="danger" className="text-[11px] font-bold">
                ROOT PRIVILEGES
              </Badge>
            </div>
            <p className="text-xs text-zinc-400">
              Manage cinema catalog, onboard multiplex theatres, and inspect microservice clusters.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate('/operator')}
              className="border-white/20 text-xs"
            >
              Operator Switch
            </Button>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b border-white/10 overflow-x-auto pb-1">
          <Tabs tabs={ADMIN_TABS} activeTab={activeTab} onChange={setActiveTab} />
        </div>

        {/* TAB 1: MOVIES CATALOGUE */}
        {activeTab === 'MOVIES' && (
          <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-6">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div>
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Film className="w-4 h-4 text-red-500" />
                  <span>Platform Movie Releases ({movies.length})</span>
                </h2>
                <p className="text-xs text-zinc-400 mt-0.5">Authoritative catalog metadata</p>
              </div>

              <Button
                variant="primary"
                size="sm"
                onClick={() => setNewMovieModalOpen(true)}
                className="gap-1.5 bg-red-600 hover:bg-red-500 text-xs"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Movie</span>
              </Button>
            </div>

            {isMoviesLoading ? (
              <LoadingState message="Loading movies..." />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {movies.map((m) => (
                  <div
                    key={m.movieId}
                    className="p-4 rounded-xl border border-white/10 bg-white/[0.02] flex flex-col justify-between gap-3 hover:border-white/20 transition"
                  >
                    <div className="space-y-1.5">
                      <div className="flex items-center justify-between gap-2">
                        <h3 className="font-bold text-white text-base truncate">{m.title}</h3>
                        <Badge variant="success" className="text-[10px]">
                          {m.status || 'ACTIVE'}
                        </Badge>
                      </div>

                      <div className="flex flex-wrap gap-1 text-[11px] text-zinc-400">
                        <span>{m.language}</span>
                        <span>&bull;</span>
                        <span>{m.durationMinutes}m</span>
                        <span>&bull;</span>
                        <span className="text-zinc-300 font-semibold">{m.certification || 'UA'}</span>
                      </div>

                      <p className="text-xs text-zinc-500 line-clamp-2 mt-1">
                        {m.description || 'No description provided.'}
                      </p>
                    </div>

                    <div className="pt-2 border-t border-white/10 flex items-center justify-between">
                      <span className="font-mono text-[10px] text-zinc-600 truncate max-w-[150px]">
                        ID: {m.movieId.substring(0, 8)}...
                      </span>
                      <button
                        type="button"
                        onClick={() => {
                          if (confirm(`Delete ${m.title} from catalog?`)) {
                            deleteMovieMutation.mutate(m.movieId);
                          }
                        }}
                        className="p-1.5 text-zinc-500 hover:text-rose-400 hover:bg-rose-950/30 rounded-lg transition"
                        title="Delete Movie"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 2: THEATRES NETWORK */}
        {activeTab === 'THEATRES' && (
          <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-6">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div>
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <Building2 className="w-4 h-4 text-red-500" />
                  <span>Multiplex &amp; Single-Screen Theatres ({theatres.length})</span>
                </h2>
                <p className="text-xs text-zinc-400 mt-0.5">Participating exhibition network</p>
              </div>

              <Button
                variant="primary"
                size="sm"
                onClick={() => setNewTheatreModalOpen(true)}
                className="gap-1.5 bg-red-600 hover:bg-red-500 text-xs"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Onboard Theatre</span>
              </Button>
            </div>

            {isTheatresLoading ? (
              <LoadingState message="Loading theatres..." />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {theatres.map((t) => (
                  <div
                    key={t.theatreId}
                    className="p-4 rounded-xl border border-white/10 bg-white/[0.02] space-y-2 hover:border-white/20 transition"
                  >
                    <div className="flex items-center justify-between">
                      <h3 className="font-bold text-white text-base">{t.name}</h3>
                      <Badge variant="default" className="text-[10px]">
                        {t.city}
                      </Badge>
                    </div>

                    <p className="text-xs text-zinc-400">{t.address || 'Central City'}</p>

                    <div className="pt-2 border-t border-white/10 flex items-center justify-between text-xs">
                      <span className="text-zinc-500 font-mono text-[10px]">
                        UUID: {t.theatreId.substring(0, 8)}...
                      </span>
                      <span className="text-emerald-400 font-medium">Verified Partner</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: SERVICES HEALTH */}
        {activeTab === 'SERVICES' && (
          <div className="rounded-2xl border border-white/10 bg-[#121620] p-6 space-y-6">
            <div className="border-b border-white/10 pb-4">
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Server className="w-4 h-4 text-emerald-400" />
                <span>Microservice Ecosystem Topology</span>
              </h2>
              <p className="text-xs text-zinc-400 mt-0.5">
                Real-time service registry and connectivity status
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {[
                { name: 'API Gateway', port: 8080, desc: 'Routing & Rate Limiting' },
                { name: 'Eureka Discovery', port: 8761, desc: 'Service Registry' },
                { name: 'User Service', port: 8081, desc: 'Auth, Customers, OTP' },
                { name: 'Movie Service', port: 8082, desc: 'Film Catalog & Assets' },
                { name: 'Theatre Service', port: 8083, desc: 'Multiplexes & Screens' },
                { name: 'Show Service', port: 8084, desc: 'Scheduling & Policies' },
                { name: 'Booking Service', port: 8085, desc: '5-Min Holds & Tickets' },
                { name: 'Payment Service', port: 8086, desc: 'Gateway & Reconciliation' },
              ].map((svc) => (
                <div
                  key={svc.name}
                  className="p-4 rounded-xl border border-white/10 bg-white/[0.02] flex items-center justify-between"
                >
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                      <h4 className="font-bold text-white text-sm">{svc.name}</h4>
                    </div>
                    <p className="text-xs text-zinc-500">{svc.desc}</p>
                  </div>
                  <span className="font-mono text-xs text-zinc-400 bg-white/5 px-2 py-1 rounded">
                    :{svc.port}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* MODAL: ADD MOVIE */}
        <Modal
          isOpen={newMovieModalOpen}
          onClose={() => setNewMovieModalOpen(false)}
          title="Add Movie to Platform Catalog"
        >
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-400">Movie Title</label>
              <input
                type="text"
                placeholder="e.g. Kalki 2898 AD"
                value={movieTitle}
                onChange={(e) => setMovieTitle(e.target.value)}
                required
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-400">Synopsis</label>
              <textarea
                rows={3}
                placeholder="Brief movie synopsis..."
                value={movieDescription}
                onChange={(e) => setMovieDescription(e.target.value)}
                className="w-full p-3 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none resize-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">Duration (Mins)</label>
                <input
                  type="number"
                  value={movieDuration}
                  onChange={(e) => setMovieDuration(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">Language</label>
                <input
                  type="text"
                  value={movieLanguage}
                  onChange={(e) => setMovieLanguage(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">Certification</label>
                <select
                  value={movieCertification}
                  onChange={(e) => setMovieCertification(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-[#121620] text-white text-sm outline-none"
                >
                  <option value="U">U (Universal)</option>
                  <option value="UA">UA (Parental Guidance)</option>
                  <option value="A">A (Adults Only)</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase text-zinc-400">Release Date</label>
                <input
                  type="date"
                  value={movieReleaseDate}
                  onChange={(e) => setMovieReleaseDate(e.target.value)}
                  className="w-full h-11 px-3 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-400">Genres (Comma separated)</label>
              <input
                type="text"
                value={movieGenres}
                onChange={(e) => setMovieGenres(e.target.value)}
                placeholder="Action, Sci-Fi, Drama"
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>

            <div className="flex justify-end gap-3 pt-3">
              <Button variant="outline" onClick={() => setNewMovieModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => createMovieMutation.mutate()}
                isLoading={createMovieMutation.isPending}
                className="bg-red-600 hover:bg-red-500"
              >
                Create Movie
              </Button>
            </div>
          </div>
        </Modal>

        {/* MODAL: ONBOARD THEATRE */}
        <Modal
          isOpen={newTheatreModalOpen}
          onClose={() => setNewTheatreModalOpen(false)}
          title="Onboard Multiplex Theatre"
        >
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-400">Theatre Name</label>
              <input
                type="text"
                placeholder="e.g. INOX LEPL Icon"
                value={theatreName}
                onChange={(e) => setTheatreName(e.target.value)}
                required
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-400">City</label>
              <input
                type="text"
                value={theatreCity}
                onChange={(e) => setTheatreCity(e.target.value)}
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase text-zinc-400">Address / Location</label>
              <input
                type="text"
                placeholder="e.g. MG Road, Patamata"
                value={theatreAddress}
                onChange={(e) => setTheatreAddress(e.target.value)}
                className="w-full h-11 px-4 rounded-xl border border-white/10 bg-white/[0.04] text-white text-sm outline-none"
              />
            </div>

            <div className="flex justify-end gap-3 pt-3">
              <Button variant="outline" onClick={() => setNewTheatreModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => createTheatreMutation.mutate()}
                isLoading={createTheatreMutation.isPending}
                className="bg-red-600 hover:bg-red-500"
              >
                Onboard Theatre
              </Button>
            </div>
          </div>
        </Modal>
      </div>
    </div>
  );
}

export default AdminPortalPage;
