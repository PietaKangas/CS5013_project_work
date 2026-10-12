import { formatDateTime } from '../utils/format';

/**
 * Shows the student (or Lead) the review outcome of a section:
 * green "verified", red "needs correction" with the Lead's comment, or blue "awaiting review".
 */
export default function ReviewNotice({ status, comment, reviewedAt, pendingText = 'The International Office will review this soon.', hidePending = false }) {
  if (status === 'VERIFIED') {
    return (
      <div className="review-notice review-notice--good" role="status">
        <strong>✓ Verified by the International Office</strong>
        {reviewedAt && <span className="muted small"> on {formatDateTime(reviewedAt)}</span>}
        {comment && <p className="review-notice__comment">“{comment}”</p>}
      </div>
    );
  }
  if (status === 'NEEDS_CORRECTION') {
    return (
      <div className="review-notice review-notice--bad" role="alert">
        <strong>Needs correction</strong>
        {reviewedAt && <span className="muted small"> · {formatDateTime(reviewedAt)}</span>}
        {comment && <p className="review-notice__comment">{comment}</p>}
      </div>
    );
  }
  if (hidePending) return null;
  return (
    <div className="review-notice review-notice--info" role="status">
      <strong>Awaiting review.</strong> {pendingText}
    </div>
  );
}
