import { useState, useCallback } from 'react';
import { CheckCircle2, AlertTriangle, XCircle, Info, X } from 'lucide-react';
import { ToastContext } from './ToastContextDefinition';

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    ({ type = 'info', title, message, duration = 4000 }) => {
      const id = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      const newToast = { id, type, title, message, duration };

      setToasts((prev) => [...prev, newToast]);

      if (duration > 0) {
        setTimeout(() => {
          removeToast(id);
        }, duration);
      }

      return id;
    },
    [removeToast]
  );

  const toast = {
    success: (message, title = 'Success', duration = 4000) =>
      showToast({ type: 'success', title, message, duration }),
    error: (message, title = 'Error', duration = 5000) =>
      showToast({ type: 'error', title, message, duration }),
    warning: (message, title = 'Warning', duration = 4000) =>
      showToast({ type: 'warning', title, message, duration }),
    info: (message, title = 'Info', duration = 4000) =>
      showToast({ type: 'info', title, message, duration }),
  };

  return (
    <ToastContext.Provider value={toast}>
      {children}
      {/* Toast Render Container */}
      <div
        aria-live="polite"
        className="fixed bottom-4 right-4 z-50 flex flex-col gap-2.5 max-w-sm w-full pointer-events-none px-4 sm:px-0"
      >
        {toasts.map((t) => (
          <ToastItem key={t.id} toast={t} onDismiss={() => removeToast(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastItem({ toast, onDismiss }) {
  const { type, title, message } = toast;

  const typeConfig = {
    success: {
      icon: <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />,
      border: 'border-emerald-500/40',
      bg: 'bg-slate-900/95',
      glow: 'shadow-emerald-950/40',
    },
    error: {
      icon: <XCircle className="w-5 h-5 text-rose-400 shrink-0" />,
      border: 'border-rose-500/40',
      bg: 'bg-slate-900/95',
      glow: 'shadow-rose-950/40',
    },
    warning: {
      icon: <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />,
      border: 'border-amber-500/40',
      bg: 'bg-slate-900/95',
      glow: 'shadow-amber-950/40',
    },
    info: {
      icon: <Info className="w-5 h-5 text-cyan-400 shrink-0" />,
      border: 'border-cyan-500/40',
      bg: 'bg-slate-900/95',
      glow: 'shadow-cyan-950/40',
    },
  };

  const config = typeConfig[type] || typeConfig.info;

  return (
    <div
      role="alert"
      className={`pointer-events-auto flex items-start gap-3 p-4 rounded-2xl border ${config.border} ${config.bg} shadow-2xl ${config.glow} backdrop-blur-md transition-all duration-300 transform translate-y-0 opacity-100`}
    >
      {config.icon}
      <div className="flex-1 min-w-0">
        {title && (
          <h4 className="text-xs font-bold text-white tracking-tight">
            {title}
          </h4>
        )}
        <p className="text-xs text-slate-300 mt-0.5 leading-relaxed break-words">
          {message}
        </p>
      </div>
      <button
        onClick={onDismiss}
        className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition cursor-pointer"
        aria-label="Dismiss notification"
      >
        <X className="w-3.5 h-3.5" />
      </button>
    </div>
  );
}

export default ToastProvider;
