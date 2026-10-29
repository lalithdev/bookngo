export function Card({
  children,
  className = '',
  variant = 'default',
  hoverEffect = false,
  onClick,
  ...props
}) {
  const baseClasses = 'rounded-2xl transition-all duration-200 border';

  const variants = {
    default:
      'bg-slate-900/90 border-slate-800/90 shadow-md shadow-black/40',
    elevated:
      'bg-slate-900 border-slate-800 shadow-xl shadow-black/60',
    glass:
      'bg-slate-900/60 backdrop-blur-md border-slate-800/80 shadow-lg',
    cinema:
      'bg-gradient-to-b from-slate-900 to-slate-950 border-slate-800 shadow-2xl shadow-black/80',
    interactive:
      'bg-slate-900/90 border-slate-800/90 hover:border-slate-700 hover:shadow-xl hover:shadow-black/50 cursor-pointer active:scale-[0.99]',
    danger:
      'bg-rose-950/20 border-rose-800/40 text-rose-200',
    warning:
      'bg-amber-950/20 border-amber-800/40 text-amber-200',
    success:
      'bg-emerald-950/20 border-emerald-800/40 text-emerald-200',
  };

  const hoverClass =
    hoverEffect && variant !== 'interactive'
      ? 'hover:border-slate-700 hover:shadow-xl hover:shadow-black/50 hover:-translate-y-0.5'
      : '';

  return (
    <div
      onClick={onClick}
      className={`${baseClasses} ${variants[variant] || variants.default} ${hoverClass} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export function CardHeader({ children, className = '', ...props }) {
  return (
    <div
      className={`p-5 pb-3 border-b border-slate-800/60 flex items-center justify-between gap-3 ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export function CardTitle({ children, className = '', ...props }) {
  return (
    <h3
      className={`text-lg font-bold text-white tracking-tight ${className}`}
      {...props}
    >
      {children}
    </h3>
  );
}

export function CardDescription({ children, className = '', ...props }) {
  return (
    <p
      className={`text-xs text-slate-400 mt-0.5 ${className}`}
      {...props}
    >
      {children}
    </p>
  );
}

export function CardContent({ children, className = '', ...props }) {
  return (
    <div className={`p-5 ${className}`} {...props}>
      {children}
    </div>
  );
}

export function CardFooter({ children, className = '', ...props }) {
  return (
    <div
      className={`p-5 pt-3 border-t border-slate-800/60 flex items-center justify-between gap-3 bg-slate-950/30 rounded-b-2xl ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export default Card;
