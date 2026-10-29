import { Link } from 'react-router';

export function Footer() {
  return (
    <footer className="bg-slate-950 border-t border-slate-900 mt-auto pt-14 pb-8 text-xs text-slate-400">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-8 pb-12 border-b border-slate-900">
          {/* Brand Info */}
          <div className="lg:col-span-2 space-y-3">
            <Link to="/" className="text-xl font-black tracking-tight text-white inline-flex items-center gap-1">
              BookNGo<span className="text-red-500">.</span>
            </Link>
            <p className="text-xs text-slate-400 max-w-sm leading-relaxed">
              High-concurrency cinema ticket booking platform. Synchronized seat holds, transactional inventory allocation, and instant digital ticketing across Vijayawada.
            </p>
            <div className="pt-2 flex items-center gap-2 text-[11px] text-emerald-400">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              <span>API Gateway Connected (Port 8080)</span>
            </div>
          </div>

          {/* Navigation */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-200">
              Navigation
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/" className="hover:text-red-400 transition-colors">
                  Now Showing
                </Link>
              </li>
              <li>
                <Link to="/movies" className="hover:text-red-400 transition-colors">
                  Movies Catalog
                </Link>
              </li>
              <li>
                <a href="#availability" className="hover:text-red-400 transition-colors">
                  Vijayawada Theatres
                </a>
              </li>
            </ul>
          </div>

          {/* Account */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-200">
              Account
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/profile" className="hover:text-red-400 transition-colors">
                  Customer Profile
                </Link>
              </li>
              <li>
                <Link to="/history" className="hover:text-red-400 transition-colors">
                  My Bookings
                </Link>
              </li>
              <li>
                <Link to="/payments/history" className="hover:text-red-400 transition-colors">
                  Payment History
                </Link>
              </li>
            </ul>
          </div>

          {/* Platform & Support */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-200">
              Platform &amp; Legal
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/tickets" className="hover:text-red-400 transition-colors">
                  Digital Tickets
                </Link>
              </li>
              <li>
                <span className="text-slate-500">5-Min Seat Hold Guarantee</span>
              </li>
              <li>
                <span className="text-slate-500">Zero Double Booking</span>
              </li>
              <li>
                <span className="text-slate-500">Terms &amp; Privacy</span>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="pt-6 flex flex-col sm:flex-row justify-between items-center text-[11px] text-slate-400 gap-3">
          <p>© {new Date().getFullYear()} BookNGo India. High-Concurrency Cinema Booking System.</p>
          <div className="flex items-center gap-4 text-slate-400">
            <span>Privacy</span>
            <span>•</span>
            <span>Terms</span>
            <span>•</span>
            <span>Security</span>
          </div>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
