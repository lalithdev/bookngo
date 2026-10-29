export function Skeleton({ className = '' }) {
  return (
    <div
      className={`animate-pulse bg-slate-800/70 rounded-xl ${className}`}
      aria-hidden="true"
    />
  );
}

export function MovieCardSkeleton() {
  return (
    <div className="bg-slate-900/90 border border-slate-800/80 rounded-2xl overflow-hidden shadow-lg">
      <Skeleton className="w-full aspect-[2/3] rounded-none" />
      <div className="p-4 space-y-3">
        <Skeleton className="h-3 w-1/3 rounded-md" />
        <Skeleton className="h-5 w-4/5 rounded-md" />
        <div className="flex gap-2 pt-1">
          <Skeleton className="h-4 w-16 rounded-lg" />
          <Skeleton className="h-4 w-20 rounded-lg" />
        </div>
        <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between">
          <Skeleton className="h-4 w-16 rounded-md" />
          <Skeleton className="h-7 w-20 rounded-lg" />
        </div>
      </div>
    </div>
  );
}

export function ShowtimeGroupSkeleton() {
  return (
    <div className="p-5 bg-slate-900/80 border border-slate-800/80 rounded-2xl space-y-4">
      <div className="flex items-center justify-between">
        <div className="space-y-1.5">
          <Skeleton className="h-5 w-48 rounded-md" />
          <Skeleton className="h-3.5 w-32 rounded-md" />
        </div>
        <Skeleton className="h-5 w-24 rounded-full" />
      </div>
      <div className="flex flex-wrap gap-3 pt-2">
        <Skeleton className="h-14 w-24 rounded-xl" />
        <Skeleton className="h-14 w-24 rounded-xl" />
        <Skeleton className="h-14 w-24 rounded-xl" />
        <Skeleton className="h-14 w-24 rounded-xl" />
      </div>
    </div>
  );
}

export function BookingCardSkeleton() {
  return (
    <div className="p-5 bg-slate-900/90 border border-slate-800/80 rounded-2xl flex flex-col sm:flex-row gap-5">
      <Skeleton className="w-24 sm:w-28 aspect-[2/3] rounded-xl shrink-0" />
      <div className="flex-1 space-y-3">
        <div className="flex items-center justify-between">
          <Skeleton className="h-5 w-40 rounded-md" />
          <Skeleton className="h-6 w-20 rounded-full" />
        </div>
        <Skeleton className="h-4 w-60 rounded-md" />
        <Skeleton className="h-4 w-44 rounded-md" />
        <div className="flex gap-2 pt-2">
          <Skeleton className="h-6 w-14 rounded-lg" />
          <Skeleton className="h-6 w-14 rounded-lg" />
        </div>
        <div className="pt-3 border-t border-slate-800 flex justify-between items-center">
          <Skeleton className="h-5 w-24 rounded-md" />
          <Skeleton className="h-8 w-28 rounded-xl" />
        </div>
      </div>
    </div>
  );
}

export function SeatMapSkeleton() {
  return (
    <div className="p-8 bg-slate-950 border border-slate-800 rounded-3xl space-y-8 flex flex-col items-center">
      <Skeleton className="h-4 w-64 rounded-full" />
      <div className="space-y-3 w-full max-w-2xl">
        {[...Array(6)].map((_, i) => (
          <div key={i} className="flex justify-center gap-2">
            {[...Array(12)].map((_, j) => (
              <Skeleton key={j} className="w-7 h-7 sm:w-8 sm:h-8 rounded-lg" />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

export default Skeleton;
