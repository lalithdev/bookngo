export function ShowtimeButton({
  time,
  format = '2D',
  screenName = null,
  pricingPreview = null,
  cancellable = true,
  disabled = false,
  selected = false,
  onClick,
  className = '',
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={disabled ? undefined : onClick}
      className={`group relative flex flex-col items-center justify-center min-w-[96px] sm:min-w-[108px] px-3.5 py-2.5 rounded-xl border transition-all duration-150 cursor-pointer select-none ${
        disabled
          ? 'opacity-40 cursor-not-allowed bg-slate-900/40 border-slate-800 text-slate-500'
          : selected
            ? 'bg-red-600 text-white border-red-500 shadow-md shadow-red-600/30'
            : 'bg-slate-900/90 text-slate-200 border-slate-800 hover:border-red-500/50 hover:bg-slate-800/90 hover:text-white'
      } ${className}`}
    >
      <span className="text-xs sm:text-sm font-bold tracking-tight">
        {time}
      </span>

      <div className="flex items-center gap-1 mt-1">
        {format && (
          <span
            className={`text-[9px] font-extrabold uppercase px-1 rounded ${
              selected
                ? 'bg-red-700/80 text-white'
                : 'bg-slate-800 text-slate-400 group-hover:text-slate-300'
            }`}
          >
            {format}
          </span>
        )}

        {screenName && (
          <span className="text-[9px] text-slate-400 truncate max-w-[60px]">
            {screenName}
          </span>
        )}

        {cancellable && (
          <span
            className="w-1.5 h-1.5 rounded-full bg-emerald-400"
            title="Free cancellation available"
          />
        )}
      </div>

      {pricingPreview && (
        <span
          className={`text-[10px] mt-0.5 font-medium ${
            selected ? 'text-red-200' : 'text-slate-400'
          }`}
        >
          {pricingPreview}
        </span>
      )}
    </button>
  );
}

export default ShowtimeButton;
