export function Chip({
  label,
  children,
  active = false,
  onClick,
  onRemove,
  icon = null,
  count = null,
  size = 'md',
  className = '',
  disabled = false,
}) {
  const content = label || children;

  const sizes = {
    sm: 'text-xs px-2.5 py-1 gap-1.5 rounded-lg',
    md: 'text-xs px-3.5 py-1.5 gap-2 rounded-xl font-medium',
    lg: 'text-sm px-4 py-2 gap-2.5 rounded-xl font-semibold',
  };

  const interactive = Boolean(onClick);

  return (
    <button
      type="button"
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      className={`inline-flex items-center select-none transition-all duration-150 border ${
        interactive
          ? 'cursor-pointer active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500'
          : 'cursor-default'
      } ${
        active
          ? 'bg-red-600 text-white border-red-500 shadow-md shadow-red-600/30'
          : 'bg-slate-900/90 text-slate-300 border-slate-800 hover:border-slate-700 hover:text-white'
      } ${disabled ? 'opacity-40 cursor-not-allowed' : ''} ${sizes[size] || sizes.md} ${className}`}
    >
      {icon && <span className="inline-flex shrink-0">{icon}</span>}
      <span>{content}</span>
      {count !== null && (
        <span
          className={`ml-1 px-1.5 py-0.2 rounded-full text-[10px] font-bold ${
            active ? 'bg-red-800/80 text-white' : 'bg-slate-800 text-slate-400'
          }`}
        >
          {count}
        </span>
      )}
      {onRemove && (
        <span
          role="button"
          tabIndex={0}
          onClick={(e) => {
            e.stopPropagation();
            onRemove();
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.stopPropagation();
              onRemove();
            }
          }}
          className="ml-1 hover:text-red-300 p-0.5 rounded cursor-pointer"
          aria-label="Remove filter"
        >
          ×
        </span>
      )}
    </button>
  );
}

export default Chip;
