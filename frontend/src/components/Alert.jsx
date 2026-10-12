/** Coloured message box. type: "error" | "success" | "info" | "warning" */
export default function Alert({ type = 'error', children, onClose }) {
  if (!children) return null;
  return (
    <div className={`alert alert--${type}`} role={type === 'error' ? 'alert' : 'status'}>
      <div className="alert__body">{children}</div>
      {onClose && (
        <button type="button" className="alert__close" onClick={onClose} aria-label="Dismiss message">
          ×
        </button>
      )}
    </div>
  );
}
