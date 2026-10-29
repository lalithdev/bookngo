import { useEffect } from 'react';
import { X } from 'lucide-react';
import { IconButton } from './IconButton';

export function Modal({
  isOpen,
  onClose,
  title,
  description,
  children,
  footer = null,
  size = 'md',
  closeOnEsc = true,
  closeOnBackdrop = true,
  showCloseButton = true,
  className = '',
}) {
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && closeOnEsc) {
        onClose();
      }
    };

    // Lock body scroll
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [isOpen, closeOnEsc, onClose]);

  if (!isOpen) return null;

  const sizes = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
    full: 'max-w-[95vw]',
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby={title ? 'modal-title' : undefined}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm transition-opacity duration-200"
        onClick={closeOnBackdrop ? onClose : undefined}
        aria-hidden="true"
      />

      {/* Modal Surface */}
      <div
        className={`relative w-full ${sizes[size] || sizes.md} bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl shadow-black/80 z-10 overflow-hidden transform transition-all duration-200 animate-in fade-in zoom-in-95 ${className}`}
      >
        {/* Header */}
        {(title || showCloseButton) && (
          <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800/80">
            <div>
              {title && (
                <h3
                  id="modal-title"
                  className="text-lg font-bold text-white tracking-tight"
                >
                  {title}
                </h3>
              )}
              {description && (
                <p className="text-xs text-slate-400 mt-0.5">{description}</p>
              )}
            </div>

            {showCloseButton && (
              <IconButton
                icon={<X className="w-4 h-4" />}
                variant="ghost"
                size="sm"
                onClick={onClose}
                ariaLabel="Close modal"
              />
            )}
          </div>
        )}

        {/* Content Body */}
        <div className="px-6 py-5 max-h-[calc(85vh-120px)] overflow-y-auto">
          {children}
        </div>

        {/* Footer */}
        {footer && (
          <div className="px-6 py-3.5 border-t border-slate-800/80 bg-slate-950/40 flex items-center justify-end gap-3">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}

export default Modal;
