export function LoadingState({
  title = 'Loading...',
  message = null,
  variant = 'section',
  className = '',
}) {
  const isFull = variant === 'full';

  return (
    <div
      role="status"
      aria-live="polite"
      className={`flex flex-col items-center justify-center p-8 text-center select-none ${
        isFull ? 'min-h-[60vh]' : 'py-16'
      } ${className}`}
    >
      {/* Cinematic Glowing Pulsing Spinner */}
      <div className="relative w-16 h-16 mb-5">
        <div className="absolute inset-0 rounded-full border-2 border-red-500/20" />
        <div className="absolute inset-0 rounded-full border-2 border-t-red-600 border-r-red-600/50 border-b-transparent border-l-transparent animate-spin" />
        <div className="absolute inset-2 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center shadow-inner">
          <div className="w-2.5 h-2.5 rounded-full bg-red-600 animate-ping opacity-75" />
        </div>
      </div>

      <h3 className="text-sm sm:text-base font-bold text-white tracking-tight">
        {title}
      </h3>

      {message && (
        <p className="text-xs text-slate-400 mt-1 max-w-xs leading-relaxed">
          {message}
        </p>
      )}
    </div>
  );
}

export default LoadingState;
