import { useMemo } from 'react';

/**
 * Reusable 7-day horizontal calendar strip for show discovery.
 * Generates ISO date strings (YYYY-MM-DD) for backend querying.
 */
export function DateSelector({
  selectedDate,
  onSelectDate,
  daysCount = 7,
  startDate = null,
  className = '',
}) {
  const dates = useMemo(() => {
    const list = [];
    const base = startDate ? new Date(startDate) : new Date();

    for (let i = 0; i < daysCount; i++) {
      const d = new Date(base);
      d.setDate(base.getDate() + i);

      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      const iso = `${year}-${month}-${day}`;

      const dayName =
        i === 0
          ? 'TODAY'
          : i === 1
            ? 'TOMORROW'
            : d.toLocaleDateString('en-US', { weekday: 'short' }).toUpperCase();

      const monthName = d.toLocaleDateString('en-US', { month: 'short' }).toUpperCase();
      const dateNum = d.getDate();

      list.push({
        iso,
        dayName,
        dateNum,
        monthName,
        isToday: i === 0,
      });
    }
    return list;
  }, [daysCount, startDate]);

  return (
    <div className={`w-full ${className}`}>
      <div className="flex items-center gap-2 overflow-x-auto pb-2 pt-1 no-scrollbar scroll-smooth">
        {dates.map((item) => {
          const isSelected = selectedDate === item.iso;
          return (
            <button
              key={item.iso}
              type="button"
              onClick={() => onSelectDate(item.iso)}
              className={`flex flex-col items-center justify-center min-w-[72px] sm:min-w-[80px] py-2.5 px-3 rounded-2xl transition-all duration-150 cursor-pointer select-none border text-center ${
                isSelected
                  ? 'bg-red-600 text-white border-red-500 shadow-lg shadow-red-600/30 scale-[1.02]'
                  : 'bg-slate-900/90 text-slate-300 border-slate-800 hover:border-slate-700 hover:bg-slate-800 hover:text-white'
              }`}
            >
              <span
                className={`text-[10px] font-bold tracking-wider ${
                  isSelected ? 'text-red-100' : 'text-slate-400'
                }`}
              >
                {item.dayName}
              </span>
              <span className="text-lg sm:text-xl font-black tracking-tight my-0.5">
                {item.dateNum}
              </span>
              <span
                className={`text-[10px] font-semibold ${
                  isSelected ? 'text-red-200' : 'text-slate-400'
                }`}
              >
                {item.monthName}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

export default DateSelector;
