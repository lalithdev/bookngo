import { Link } from 'react-router';
import { Button } from './Button';

export function PhasePlaceholder({
  phase = 'Phase 2',
  title = 'Feature In Development',
  description = 'This screen is scheduled for implementation in a future phase of BookNGo.',
}) {
  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center text-center px-4 py-16">
      <div className="w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-500 mb-6 shadow-lg shadow-red-500/10">
        <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
        </svg>
      </div>

      <div className="inline-block px-3 py-1 rounded-full bg-slate-800 text-xs font-semibold text-gray-300 border border-slate-700 mb-4">
        {phase}
      </div>

      <h1 className="text-2xl md:text-3xl font-bold text-gray-100 mb-3 tracking-tight">
        {title}
      </h1>

      <p className="text-gray-400 text-sm md:text-base max-w-md mb-8">
        {description}
      </p>

      <div className="flex gap-4">
        <Link to="/movies">
          <Button variant="primary">Browse Movies</Button>
        </Link>
        <Link to="/">
          <Button variant="secondary">Home</Button>
        </Link>
      </div>
    </div>
  );
}
