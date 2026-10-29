import { Check } from 'lucide-react';

const DEFAULT_STEPS = [
  { step: 1, label: 'CHOOSE A MOVIE', shortLabel: 'Movie' },
  { step: 2, label: 'CHOOSE SEATS', shortLabel: 'Seats' },
  { step: 3, label: 'PURCHASE', shortLabel: 'Purchase' },
  { step: 4, label: 'COMPLETE', shortLabel: 'Ticket' },
];

export function ProgressIndicator({
  currentStep = 1,
  steps = DEFAULT_STEPS,
  className = '',
}) {
  return (
    <nav
      aria-label="Booking Progress"
      className={`w-full py-4 border-b border-slate-800/80 bg-slate-950/60 backdrop-blur-md ${className}`}
    >
      <div className="max-w-4xl mx-auto px-4 sm:px-6">
        <ol className="flex items-center justify-between relative">
          {/* Background connecting line */}
          <div className="absolute left-6 right-6 top-1/2 -translate-y-1/2 h-0.5 bg-slate-800 -z-0" />

          {steps.map((item) => {
            const isCompleted = item.step < currentStep;
            const isCurrent = item.step === currentStep;

            return (
              <li
                key={item.step}
                className="relative z-10 flex flex-col items-center group"
              >
                <div
                  className={`w-8 h-8 sm:w-9 sm:h-9 rounded-full flex items-center justify-center font-bold text-xs transition-all duration-200 ${
                    isCompleted
                      ? 'bg-red-600 text-white shadow-md shadow-red-600/30'
                      : isCurrent
                        ? 'bg-slate-900 border-2 border-red-500 text-red-500 shadow-lg shadow-red-600/20 scale-110'
                        : 'bg-slate-900 border border-slate-800 text-slate-500'
                  }`}
                >
                  {isCompleted ? (
                    <Check className="w-4 h-4 stroke-[3]" />
                  ) : (
                    <span>0{item.step}</span>
                  )}
                </div>

                <span
                  className={`text-[10px] sm:text-xs font-bold tracking-wider uppercase mt-2 text-center transition-colors ${
                    isCurrent
                      ? 'text-white'
                      : isCompleted
                        ? 'text-slate-300'
                        : 'text-slate-500'
                  }`}
                >
                  <span className="hidden sm:inline">{item.label}</span>
                  <span className="sm:hidden">{item.shortLabel || item.label}</span>
                </span>
              </li>
            );
          })}
        </ol>
      </div>
    </nav>
  );
}

export default ProgressIndicator;
