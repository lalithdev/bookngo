import { Film } from 'lucide-react';
import { Button } from './Button';

export function EmptyState({
  title = 'No items found',
  description = 'There are no records matching your criteria.',
  actionLabel,
  onAction,
  actionHref,
  icon,
  children,
  className = '',
}) {
  return (
    <div
      className={`flex flex-col items-center justify-center p-8 sm:p-12 text-center bg-slate-900/60 border border-slate-800/80 rounded-3xl max-w-lg mx-auto my-8 shadow-xl ${className}`}
    >
      <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center text-slate-400 mb-5 border border-slate-700/60 shadow-inner shadow-black/40">
        {icon || <Film className="w-8 h-8 text-slate-500" />}
      </div>

      <h3 className="text-lg sm:text-xl font-bold text-white mb-2 tracking-tight">
        {title}
      </h3>

      <p className="text-xs sm:text-sm text-slate-400 mb-6 max-w-sm leading-relaxed">
        {description}
      </p>

      {children}

      {actionLabel && (
        <div>
          {actionHref ? (
            <a href={actionHref}>
              <Button variant="primary" size="md">
                {actionLabel}
              </Button>
            </a>
          ) : (
            <Button variant="primary" size="md" onClick={onAction}>
              {actionLabel}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

export default EmptyState;
