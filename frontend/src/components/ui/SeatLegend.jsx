export function SeatLegend({
  categories = [],
  currencySymbol = '₹',
  className = '',
}) {
  return (
    <div
      className={`flex flex-wrap items-center justify-center gap-4 sm:gap-8 py-3 px-4 bg-slate-950/60 border border-slate-800/80 rounded-2xl text-xs ${className}`}
    >
      {/* Available */}
      <div className="flex items-center gap-2">
        <div className="w-5 h-5 rounded-lg border-2 border-slate-600 bg-slate-900/60" />
        <span className="text-slate-300 font-medium">Available</span>
      </div>

      {/* Selected */}
      <div className="flex items-center gap-2">
        <div className="w-5 h-5 rounded-lg bg-red-600 border-2 border-red-500 shadow-md shadow-red-600/30" />
        <span className="text-white font-bold">Selected</span>
      </div>

      {/* Held by Others */}
      <div className="flex items-center gap-2">
        <div className="w-5 h-5 rounded-lg border-2 border-amber-500/60 bg-amber-950/30 relative overflow-hidden">
          <div className="absolute inset-0 bg-[linear-gradient(45deg,transparent_25%,rgba(245,158,11,0.3)_50%,transparent_75%)] bg-[length:6px_6px]" />
        </div>
        <span className="text-amber-400 font-medium">Held</span>
      </div>

      {/* Booked / Sold */}
      <div className="flex items-center gap-2">
        <div className="w-5 h-5 rounded-lg bg-slate-800/60 border border-slate-700/40 opacity-60" />
        <span className="text-slate-500 font-medium">Booked</span>
      </div>

      {/* Category tiers if provided */}
      {categories.length > 0 && (
        <div className="hidden md:flex items-center gap-3 pl-4 border-l border-slate-800">
          {categories.map((cat) => (
            <span
              key={cat.tier || cat.name}
              className="text-[11px] font-semibold text-slate-400"
            >
              <span className="text-slate-200">{cat.tier || cat.name}:</span>{' '}
              <span className="text-red-400">
                {currencySymbol}{cat.price}
              </span>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

export default SeatLegend;
