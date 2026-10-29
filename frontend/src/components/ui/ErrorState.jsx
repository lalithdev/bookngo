import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from './Button';

export function ErrorState({
  title = 'Something went wrong',
  message = 'We encountered an issue communicating with BookNGo services.',
  onRetry,
  variant = 'card',
  code = null,
  className = '',
}) {
  if (variant === 'banner') {
    return (
      <div
        role="alert"
        className={`p-4 rounded-xl bg-rose-950/40 border border-rose-600/40 flex items-center justify-between gap-4 text-rose-200 ${className}`}
      >
        <div className="flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
          <div>
            <div className="text-xs sm:text-sm font-semibold">{title}</div>
            {message && <div className="text-xs text-rose-300/80">{message}</div>}
          </div>
        </div>
        {onRetry && (
          <Button
            variant="danger"
            size="xs"
            onClick={onRetry}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            Retry
          </Button>
        )}
      </div>
    );
  }

  return (
    <div
      role="alert"
      className={`flex flex-col items-center justify-center p-8 text-center bg-rose-950/20 border border-rose-500/30 rounded-3xl max-w-lg mx-auto my-8 shadow-xl ${className}`}
    >
      <div className="w-14 h-14 rounded-2xl bg-rose-500/10 flex items-center justify-center text-rose-400 mb-4 border border-rose-500/20 shadow-md">
        <AlertTriangle className="w-7 h-7" />
      </div>

      <h3 className="text-base sm:text-lg font-bold text-rose-100 mb-1.5 tracking-tight">
        {title}
      </h3>

      <p className="text-xs sm:text-sm text-slate-300 mb-5 max-w-sm leading-relaxed">
        {message}
      </p>

      {code && (
        <span className="text-[10px] font-mono bg-slate-900 px-2 py-1 rounded text-slate-400 mb-4 border border-slate-800">
          Ref: {code}
        </span>
      )}

      {onRetry && (
        <Button
          variant="danger"
          size="sm"
          onClick={onRetry}
          leftIcon={<RefreshCw className="w-4 h-4" />}
          className="shadow-lg shadow-rose-900/30"
        >
          Try Again
        </Button>
      )}
    </div>
  );
}

export default ErrorState;
