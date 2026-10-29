export function PriceSummary({
  items = [],
  subtotal = 0,
  convenienceFee = 0,
  taxAmount = 0,
  totalAmount = 0,
  currencySymbol = '₹',
  isBreakdownVisible = true,
  className = '',
}) {
  const displayTotal = totalAmount || (subtotal + convenienceFee + taxAmount);

  return (
    <div
      className={`p-5 bg-slate-900/90 border border-slate-800 rounded-2xl space-y-4 shadow-lg ${className}`}
    >
      <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider pb-2 border-b border-slate-800/80">
        Order Summary
      </h4>

      {/* Itemized seats/tiers */}
      {isBreakdownVisible && items.length > 0 && (
        <div className="space-y-2 text-xs">
          {items.map((item, index) => (
            <div key={index} className="flex justify-between items-center text-slate-300">
              <span className="truncate pr-2">
                {item.label} {item.count ? `(×${item.count})` : ''}
              </span>
              <span className="font-semibold shrink-0 text-white">
                {currencySymbol}{item.amount}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Price Subtotals */}
      <div className="space-y-1.5 text-xs text-slate-400 pt-2 border-t border-slate-800/60">
        <div className="flex justify-between">
          <span>Tickets Subtotal</span>
          <span className="text-slate-200">
            {currencySymbol}{subtotal}
          </span>
        </div>

        {convenienceFee > 0 && (
          <div className="flex justify-between">
            <span>Convenience Fee</span>
            <span className="text-slate-200">
              {currencySymbol}{convenienceFee}
            </span>
          </div>
        )}

        {taxAmount > 0 && (
          <div className="flex justify-between">
            <span>Taxes &amp; GST</span>
            <span className="text-slate-200">
              {currencySymbol}{taxAmount}
            </span>
          </div>
        )}
      </div>

      {/* Grand Total */}
      <div className="pt-3 border-t border-slate-800 flex items-baseline justify-between">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 block">
            Total Payable
          </span>
          <span className="text-[10px] text-slate-500">
            Authoritative Server Price
          </span>
        </div>

        <div className="text-xl sm:text-2xl font-black text-white tracking-tight text-right">
          <span className="text-red-500 mr-0.5">{currencySymbol}</span>
          {displayTotal}
        </div>
      </div>
    </div>
  );
}

export default PriceSummary;
