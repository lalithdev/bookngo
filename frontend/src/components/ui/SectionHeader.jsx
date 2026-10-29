export function SectionHeader({
  title,
  subtitle,
  badge = null,
  action = null,
  icon = null,
  className = '',
}) {
  return (
    <div
      className={`flex flex-col sm:flex-row sm:items-end justify-between gap-3 mb-6 pb-2 ${className}`}
    >
      <div>
        <div className="flex items-center gap-2.5">
          {icon && (
            <span className="p-1.5 rounded-lg bg-red-600/10 border border-red-500/20 text-red-500">
              {icon}
            </span>
          )}
          <h2 className="text-xl sm:text-2xl font-black tracking-tight text-white flex items-center gap-2">
            <span>{title}</span>
            {badge && <span>{badge}</span>}
          </h2>
        </div>
        {subtitle && (
          <p className="text-xs sm:text-sm text-slate-400 mt-1 max-w-2xl">
            {subtitle}
          </p>
        )}
      </div>

      {action && (
        <div className="flex items-center gap-2 shrink-0 self-start sm:self-auto">
          {action}
        </div>
      )}
    </div>
  );
}

export default SectionHeader;
