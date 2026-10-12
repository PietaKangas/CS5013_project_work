import { useEffect, useRef } from 'react';

/**
 * Confirmation popup, e.g. before deleting a document.
 *   <ConfirmDialog open={...} title="Delete document?" message="..." danger
 *                  onConfirm={...} onCancel={...} />
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  danger = false,
  busy = false,
  onConfirm,
  onCancel,
}) {
  const cancelRef = useRef(null);
  const latest = useRef({ busy, onCancel });
  latest.current = { busy, onCancel };

  // Runs only when the dialog opens or closes.
  useEffect(() => {
    if (!open) return undefined;
    cancelRef.current?.focus(); // safest button gets focus first
    const onKey = (e) => {
      if (e.key === 'Escape' && !latest.current.busy) latest.current.onCancel();
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open]);

  if (!open) return null;

  return (
    <div className="modal-backdrop" onMouseDown={() => !busy && onCancel()}>
      <div
        className="modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby="confirm-message"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2 id="confirm-title" className="modal__title">{title}</h2>
        <div id="confirm-message" className="modal__message">{message}</div>
        <div className="modal__actions">
          <button ref={cancelRef} type="button" className="btn btn--ghost" onClick={onCancel} disabled={busy}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={danger ? 'btn btn--danger' : 'btn btn--primary'}
            onClick={onConfirm}
            disabled={busy}
          >
            {busy ? 'Please wait…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
