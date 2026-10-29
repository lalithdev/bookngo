import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router';
import {
  MapPin,
  Search,
  Film,
  Menu,
  X,
  ChevronDown,
} from 'lucide-react';
import { useAuth } from '../../context/useAuth';
import { Button } from '../ui/Button';
import { AccountDrawer } from './AccountDrawer';

const SUPPORTED_CITIES = [
  'Vijayawada',
  'Hyderabad',
  'Bengaluru',
  'Mumbai',
  'Chennai',
  'Delhi NCR',
];

export function Navbar({ selectedCity = 'Vijayawada', onCityChange }) {
  const { isAuthenticated, user } = useAuth();
  const [searchQuery, setSearchQuery] = useState('');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [accountDrawerOpen, setAccountDrawerOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/movies?title=${encodeURIComponent(searchQuery.trim())}`);
      setSearchQuery('');
      setMobileMenuOpen(false);
    }
  };

  const displayName = user?.fullName || 'Customer';
  const displayInitials = displayName
    ? displayName
        .split(' ')
        .map((n) => n[0])
        .join('')
        .slice(0, 2)
        .toUpperCase()
    : 'U';

  const isMoviesActive = location.pathname.startsWith('/movies');

  return (
    <>
      <header className="sticky top-0 z-40 bg-slate-950/95 backdrop-blur-md border-b border-slate-800/70 shadow-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14 sm:h-16 gap-4">
            {/* Left: Brand & City Selector */}
            <div className="flex items-center gap-3 sm:gap-4 shrink-0">
              <Link to="/" className="flex items-center gap-2 group shrink-0">
                <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-red-600 to-red-700 flex items-center justify-center shadow-md shadow-red-600/30 group-hover:from-red-500 group-hover:to-red-600 transition border border-red-500/30">
                  <Film className="w-4 h-4 text-white" />
                </div>
                <span className="text-xl font-black tracking-tight text-white flex items-center font-heading">
                  BookNGo<span className="text-red-500">.</span>
                </span>
              </Link>

              {/* City Selector */}
              <div className="hidden sm:flex items-center gap-1.5 text-xs text-slate-300 bg-slate-900/90 border border-slate-800/90 px-3 py-1.5 rounded-xl hover:border-slate-700 transition">
                <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0" />
                <select
                  value={selectedCity}
                  onChange={(e) => onCityChange && onCityChange(e.target.value)}
                  className="bg-transparent text-slate-200 font-semibold text-xs focus:outline-none cursor-pointer pr-1"
                  aria-label="Select City"
                >
                  {SUPPORTED_CITIES.map((city) => (
                    <option key={city} value={city} className="bg-slate-900 text-slate-200">
                      {city}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Center: Search Bar (Desktop) */}
            <form
              onSubmit={handleSearchSubmit}
              className="hidden md:flex flex-1 max-w-sm lg:max-w-md mx-auto relative"
            >
              <div className="relative w-full">
                <input
                  type="text"
                  placeholder="Search movies, languages, or titles..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full bg-slate-900/80 border border-slate-800 rounded-xl pl-9 pr-4 py-1.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-red-500 focus:border-red-500/80 transition"
                />
                <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2" />
              </div>
            </form>

            {/* Right: Movies Link & Profile / Sign In */}
            <div className="hidden sm:flex items-center gap-3 shrink-0">
              <Link
                to="/movies"
                className={`text-xs font-semibold transition px-3 py-1.5 rounded-xl ${
                  isMoviesActive
                    ? 'bg-red-600/15 text-red-400 border border-red-500/30'
                    : 'text-slate-300 hover:text-white hover:bg-slate-900/80'
                }`}
              >
                Movies
              </Link>

              {isAuthenticated ? (
                /* Authenticated Profile Trigger */
                <button
                  onClick={() => setAccountDrawerOpen(true)}
                  className="flex items-center gap-2 pl-2 pr-3 py-1 rounded-full bg-slate-900 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 transition cursor-pointer group"
                  aria-label="Open Account Menu"
                >
                  <div className="w-7 h-7 rounded-full bg-gradient-to-br from-red-600 to-red-800 flex items-center justify-center text-white text-xs font-black shadow-sm shadow-red-600/30">
                    {displayInitials}
                  </div>
                  <span className="text-xs font-semibold text-slate-200 group-hover:text-white max-w-[110px] truncate">
                    {user?.fullName || 'Profile'}
                  </span>
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400 group-hover:text-slate-200 transition-transform" />
                </button>
              ) : (
                /* Unauthenticated Sign In */
                <Link to="/login">
                  <Button variant="primary" size="sm" className="font-bold px-4 text-xs h-8">
                    Sign In
                  </Button>
                </Link>
              )}
            </div>

            {/* Mobile Actions */}
            <div className="flex sm:hidden items-center gap-2">
              {isAuthenticated && (
                <button
                  onClick={() => setAccountDrawerOpen(true)}
                  className="w-7 h-7 rounded-full bg-red-600 flex items-center justify-center text-white text-xs font-bold shadow"
                  aria-label="Open Account Menu"
                >
                  {displayInitials}
                </button>
              )}

              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="p-1.5 rounded-xl text-slate-400 hover:text-white hover:bg-slate-900 focus:outline-none"
                aria-label="Toggle Navigation Menu"
              >
                {mobileMenuOpen ? (
                  <X className="w-5 h-5" />
                ) : (
                  <Menu className="w-5 h-5" />
                )}
              </button>
            </div>
          </div>

          {/* Mobile Menu Dropdown */}
          {mobileMenuOpen && (
            <div className="sm:hidden border-t border-slate-800/80 py-3 space-y-3">
              <form onSubmit={handleSearchSubmit} className="relative">
                <input
                  type="text"
                  placeholder="Search movies..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-9 pr-4 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-red-500"
                />
                <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
              </form>

              <div className="flex items-center justify-between text-xs text-slate-300 py-1">
                <span className="flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-red-500" />
                  City:
                </span>
                <select
                  value={selectedCity}
                  onChange={(e) => onCityChange && onCityChange(e.target.value)}
                  className="bg-slate-900 border border-slate-800 text-slate-200 rounded-lg px-2 py-1 font-medium text-xs"
                >
                  {SUPPORTED_CITIES.map((city) => (
                    <option key={city} value={city}>
                      {city}
                    </option>
                  ))}
                </select>
              </div>

              <div className="pt-1 flex flex-col gap-1.5">
                <Link
                  to="/movies"
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-xs font-semibold text-slate-200 hover:text-white py-1.5 px-2 rounded-lg hover:bg-slate-900"
                >
                  Browse Movies
                </Link>

                {isAuthenticated ? (
                  <button
                    onClick={() => {
                      setMobileMenuOpen(false);
                      setAccountDrawerOpen(true);
                    }}
                    className="text-left text-xs font-semibold text-slate-200 hover:text-white py-1.5 px-2 rounded-lg hover:bg-slate-900"
                  >
                    My Account &amp; Bookings
                  </button>
                ) : (
                  <Link to="/login" onClick={() => setMobileMenuOpen(false)}>
                    <Button variant="primary" size="sm" className="w-full mt-1 text-xs">
                      Sign In
                    </Button>
                  </Link>
                )}
              </div>
            </div>
          )}
        </div>
      </header>

      {/* Account Slide-Over Drawer */}
      <AccountDrawer
        isOpen={accountDrawerOpen}
        onClose={() => setAccountDrawerOpen(false)}
      />
    </>
  );
}

export default Navbar;
