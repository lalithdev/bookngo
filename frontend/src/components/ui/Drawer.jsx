import { useEffect } from 'react';
import { X } from 'lucide-react';
import { IconButton } from './IconButton';

export function Drawer({
  isOpen,
  onClose,
  title,
  description,
  children,
  footer = null,
  position = 'right',
  width = 'max-w-md',
  closeOnEsc = true,
  className = '',
}) {
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && closeOnEsc) {
        onClose();
      }
    };

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [isOpen, closeOnEsc, onClose]);

  if (!isOpen) return null;

  const positionClasses = {
    right: 'inset-y-0 right-0',
    left: 'inset-y-0 left-0',
    bottom: 'inset-x-0 bottom-0 max-h-[85vh] rounded-t-2xl',
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 overflow-hidden"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      <div
        className={`fixed ${positionClasses[position] || positionClasses.right} ${
          position !== 'bottom' ? `w-full ${width}` : 'w-full'
        } bg-slate-900 border-l border-slate-800 shadow-2xl flex flex-col z-10 ${className}`}
      >
        {/* Drawer Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800/80 bg-slate-950/30">
          <div>
            {title && (
              <h3 className="text-base font-bold text-white tracking-tight">
                {title}
              </h3>
            )}
            {description && (
              <p className="text-xs text-slate-400 mt-0.5">{description}</p>
            )}
          </div>
          <IconButton
            icon={<X className="w-4 h-4" />}
            variant="ghost"
            size="sm"
            onClick={onClose}
            ariaLabel="Close drawer"
          />
        </div>

        {/* Drawer Body */}
        <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>

        {/* Drawer Footer */}
        {footer && (
          <div className="px-6 py-4 border-t border-slate-800/80 bg-slate-950/50">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}

export default Drawer;
