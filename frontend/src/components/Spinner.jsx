export default function Spinner({ label = 'Loading…', inline = false }) {
  return (
    <div className={inline ? 'spinner spinner--inline' : 'spinner'} role="status" aria-live="polite">
      <span className="spinner__circle" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
