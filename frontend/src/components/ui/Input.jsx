export function Input({
  label,
  error,
  id,
  className = '',
  disabled = false,
  ...props
}) {
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  return (
    <div className="w-full space-y-1.5">
      {label && (
        <label
          htmlFor={inputId}
          className="block text-xs font-medium text-gray-300"
        >
          {label}
        </label>
      )}
      <input
        id={inputId}
        disabled={disabled}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${inputId}-error` : undefined}
        className={`w-full bg-slate-900 border ${
          error ? 'border-rose-500 focus:ring-rose-500' : 'border-slate-700 focus:ring-red-500'
        } rounded-lg px-4 py-2.5 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:border-transparent transition disabled:opacity-50 disabled:cursor-not-allowed ${className}`}
        {...props}
      />
      {error && (
        <p id={`${inputId}-error`} className="text-xs text-rose-400 mt-1">
          {error}
        </p>
      )}
    </div>
  );
}
