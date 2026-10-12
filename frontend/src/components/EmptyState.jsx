export default function EmptyState({ title, children }) {
  return (
    <div className="empty">
      <p className="empty__title">{title}</p>
      {children && <div className="empty__text">{children}</div>}
    </div>
  );
}
