export function Button({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled = false,
  leftIcon = null,
  rightIcon = null,
  className = '',
  type = 'button',
  ...props
}) {
  const baseClasses =
    'inline-flex items-center justify-center font-medium transition-all duration-150 ease-in-out rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer select-none active:scale-[0.98]';

  const variants = {
    primary:
      'bg-red-600 hover:bg-red-500 text-white shadow-lg shadow-red-600/25 active:bg-red-700 border border-red-500/30',
    secondary:
      'bg-slate-900 hover:bg-slate-800 text-slate-200 border border-slate-700/80 active:bg-slate-950 shadow-sm',
    outline:
      'bg-transparent hover:bg-red-600/10 text-red-500 border border-red-600/60 hover:border-red-500 active:bg-red-950/20',
    ghost:
      'bg-transparent hover:bg-slate-800/80 text-slate-300 hover:text-white active:bg-slate-800',
    danger:
      'bg-rose-600 hover:bg-rose-500 text-white shadow-lg shadow-rose-600/25 active:bg-rose-700 border border-rose-500/30',
    gold:
      'bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold shadow-lg shadow-amber-500/20 border border-amber-400/40',
    cinema:
      'bg-gradient-to-r from-red-600 via-rose-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white font-bold shadow-xl shadow-red-900/40 border border-red-500/40',
  };

  const sizes = {
    xs: 'text-xs px-2.5 py-1 gap-1.5 rounded-lg',
    sm: 'text-xs px-3.5 py-1.5 gap-2 rounded-lg font-medium',
    md: 'text-sm px-4 py-2 gap-2 rounded-xl font-semibold',
    lg: 'text-base px-6 py-3 gap-2.5 rounded-xl font-bold tracking-wide',
  };

  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      className={`${baseClasses} ${variants[variant] || variants.primary} ${sizes[size] || sizes.md} ${className}`}
      {...props}
    >
      {isLoading ? (
        <svg
          className="animate-spin -ml-0.5 h-4 w-4 text-current"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      ) : (
        leftIcon && <span className="inline-flex shrink-0">{leftIcon}</span>
      )}
      <span>{children}</span>
      {!isLoading && rightIcon && (
        <span className="inline-flex shrink-0">{rightIcon}</span>
      )}
    </button>
  );
}

export default Button;
