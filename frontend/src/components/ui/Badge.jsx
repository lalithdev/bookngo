export function Badge({
  children,
  variant = 'default',
  size = 'md',
  dot = false,
  className = '',
}) {
  const baseClasses =
    'inline-flex items-center font-medium transition-colors select-none tracking-wide';

  const variants = {
    default: 'bg-slate-800/90 text-slate-300 border border-slate-700/80',
    brand: 'bg-red-500/10 text-red-400 border border-red-500/30',
    primary: 'bg-red-600 text-white shadow-sm shadow-red-600/30',
    success: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30',
    warning: 'bg-amber-500/10 text-amber-300 border border-amber-500/30',
    danger: 'bg-rose-500/10 text-rose-400 border border-rose-500/30',
    info: 'bg-cyan-500/10 text-cyan-300 border border-cyan-500/30',
    gold: 'bg-amber-500/15 text-amber-300 border border-amber-500/40 shadow-sm shadow-amber-500/10',
    vip: 'bg-gradient-to-r from-amber-500/20 to-amber-600/20 text-amber-300 border border-amber-500/40',
    premium: 'bg-purple-500/15 text-purple-300 border border-purple-500/30',
    regular: 'bg-slate-800 text-slate-300 border border-slate-700',
    outline: 'bg-transparent text-slate-400 border border-slate-700',
    imax: 'bg-blue-600/20 text-blue-400 border border-blue-500/40 font-black tracking-wider',
  };

  const sizes = {
    xs: 'text-[10px] px-1.5 py-0.5 rounded gap-1',
    sm: 'text-[11px] px-2 py-0.5 rounded-md gap-1.5',
    md: 'text-xs px-2.5 py-0.5 rounded-lg gap-1.5',
    lg: 'text-sm px-3 py-1 rounded-lg gap-2 font-semibold',
  };

  const dotColors = {
    default: 'bg-slate-400',
    brand: 'bg-red-400',
    primary: 'bg-white',
    success: 'bg-emerald-400',
    warning: 'bg-amber-400',
    danger: 'bg-rose-400',
    info: 'bg-cyan-400',
    gold: 'bg-amber-400',
    vip: 'bg-amber-400',
    premium: 'bg-purple-400',
    regular: 'bg-slate-400',
    outline: 'bg-slate-400',
    imax: 'bg-blue-400',
  };

  return (
    <span
      className={`${baseClasses} ${variants[variant] || variants.default} ${sizes[size] || sizes.md} ${className}`}
    >
      {dot && (
        <span
          className={`w-1.5 h-1.5 rounded-full ${dotColors[variant] || 'bg-slate-400'}`}
        />
      )}
      {children}
    </span>
  );
}

export default Badge;
