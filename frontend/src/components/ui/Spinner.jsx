export function Spinner({ size = 'md', className = '' }) {
  const sizes = {
    sm: 'h-4 w-4 border-2',
    md: 'h-8 w-8 border-3',
    lg: 'h-12 w-12 border-4',
  };

  return (
    <div
      role="status"
      className={`animate-spin rounded-full border-slate-700 border-t-red-600 ${sizes[size] || sizes.md} ${className}`}
    >
      <span className="sr-only">Loading...</span>
    </div>
  );
}
