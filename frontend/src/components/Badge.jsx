/** Small coloured label. tone: "good" | "warn" | "bad" | "neutral" | "info" */
export default function Badge({ tone = 'neutral', children }) {
  return <span className={`badge badge--${tone}`}>{children}</span>;
}

export function CompletionBadge({ complete, percent }) {
  if (complete) return <Badge tone="good">Complete</Badge>;
  return <Badge tone={percent >= 50 ? 'warn' : 'bad'}>{percent}%</Badge>;
}

export function HostelBadge({ status }) {
  const tone = { ALLOCATED: 'good', APPLIED: 'info', NOT_APPLIED: 'neutral' }[status] || 'neutral';
  const label = { ALLOCATED: 'Allocated', APPLIED: 'Applied', NOT_APPLIED: 'Not applied' }[status] || status;
  return <Badge tone={tone}>{label}</Badge>;
}

export function SlotStatusBadge({ status, past }) {
  if (status === 'CANCELLED') return <Badge tone="bad">Cancelled</Badge>;
  if (status === 'BOOKED') return <Badge tone="info">{past ? 'Completed' : 'Booked'}</Badge>;
  return <Badge tone={past ? 'neutral' : 'good'}>{past ? 'Expired' : 'Available'}</Badge>;
}

/** Review of a profile or hostel details: PENDING | VERIFIED | NEEDS_CORRECTION. */
export function ReviewBadge({ status }) {
  if (status === 'VERIFIED') return <Badge tone="good">✓ Verified</Badge>;
  if (status === 'NEEDS_CORRECTION') return <Badge tone="bad">Needs correction</Badge>;
  return <Badge tone="info">Awaiting review</Badge>;
}

/** Status of one required document (document is null when it hasn't been uploaded). */
export function DocumentStatusBadge({ document }) {
  if (!document) return <Badge tone="neutral">Missing</Badge>;
  if (document.status === 'VERIFIED') return <Badge tone="good">✓ Verified</Badge>;
  if (document.status === 'NEEDS_CORRECTION') return <Badge tone="bad">Needs correction</Badge>;
  return <Badge tone="info">Awaiting review</Badge>;
}

/** One-word summary of a whole student, for the Lead's list. */
export function ReviewStateBadge({ state }) {
  const map = {
    AWAITING_REVIEW: ['info', 'Awaiting review'],
    NEEDS_CORRECTION: ['bad', 'Needs correction'],
    INCOMPLETE: ['warn', 'Incomplete'],
    VERIFIED: ['good', '✓ Fully verified'],
  };
  const [tone, label] = map[state] || ['neutral', state];
  return <Badge tone={tone}>{label}</Badge>;
}
