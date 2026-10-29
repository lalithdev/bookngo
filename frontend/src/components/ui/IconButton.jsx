export function IconButton({
  icon,
  children,
  variant = 'secondary',
  size = 'md',
  disabled = false,
  isLoading = false,
  className = '',
  type = 'button',
  ariaLabel,
  ...props
}) {
  const baseClasses =
    'inline-flex items-center justify-center transition-all duration-150 rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer select-none active:scale-95';

  const variants = {
    primary:
      'bg-red-600 hover:bg-red-500 text-white shadow-md shadow-red-600/30 border border-red-500/40',
    secondary:
      'bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-800 hover:border-slate-700 shadow-sm',
    ghost:
      'bg-transparent hover:bg-slate-800/80 text-slate-400 hover:text-white',
    danger:
      'bg-rose-600/20 hover:bg-rose-600 text-rose-400 hover:text-white border border-rose-500/30',
    outline:
      'bg-transparent hover:bg-slate-800 text-slate-300 border border-slate-700 hover:border-slate-600',
  };

  const sizes = {
    xs: 'w-7 h-7 text-xs',
    sm: 'w-8 h-8 text-sm',
    md: 'w-10 h-10 text-base',
    lg: 'w-12 h-12 text-lg',
  };

  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      aria-label={ariaLabel}
      className={`${baseClasses} ${variants[variant] || variants.secondary} ${sizes[size] || sizes.md} ${className}`}
      {...props}
    >
      {isLoading ? (
        <svg
          className="animate-spin h-4 w-4 text-current"
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
        icon || children
      )}
    </button>
  );
}

export default IconButton;
