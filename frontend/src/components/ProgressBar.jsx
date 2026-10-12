export default function ProgressBar({ percent, label }) {
  const value = Math.max(0, Math.min(100, Math.round(percent ?? 0)));
  const tone = value === 100 ? 'good' : value >= 50 ? 'warn' : 'bad';
  return (
    <div className="progress">
      {label && (
        <div className="progress__label">
          <span>{label}</span>
          <span>{value}%</span>
        </div>
      )}
      <div
        className="progress__track"
        role="progressbar"
        aria-valuenow={value}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={label}
      >
        <div className={`progress__fill progress__fill--${tone}`} style={{ width: `${value}%` }} />
      </div>
    </div>
  );
}
