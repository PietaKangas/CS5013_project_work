// Display helpers shared by many pages.

export const HOSTEL_LABELS = {
  NOT_APPLIED: 'Not applied',
  APPLIED: 'Applied',
  ALLOCATED: 'Allocated',
};

export const CATEGORY_LABELS = {
  DEGREE: 'Full-time degree student',
  EXCHANGE: 'Semester exchange student',
};

export const GENDER_LABELS = {
  MALE: 'Male',
  FEMALE: 'Female',
  OTHER: 'Other',
};

/** Lead review of a profile / hostel details. */
export const REVIEW_LABELS = {
  PENDING: 'Awaiting review',
  VERIFIED: 'Verified',
  NEEDS_CORRECTION: 'Needs correction',
};

/** One-word summary of a student for the Lead (see StudentSummary.java). */
export const REVIEW_STATE_LABELS = {
  AWAITING_REVIEW: 'Awaiting review',
  NEEDS_CORRECTION: 'Needs correction',
  INCOMPLETE: 'Incomplete',
  VERIFIED: 'Fully verified',
};

/** "2026-11-02" -> "Mon, 2 Nov 2026" */
export function formatDate(isoDate) {
  if (!isoDate) return '—';
  const d = new Date(`${isoDate}T00:00:00`);
  return d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
}

/** "10:00:00" -> "10:00" */
export function formatTime(isoTime) {
  return isoTime ? isoTime.slice(0, 5) : '';
}

/** "2026-10-11T07:30:00Z" -> local date and time */
export function formatDateTime(instant) {
  if (!instant) return '—';
  return new Date(instant).toLocaleString(undefined, {
    day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

export function formatBytes(bytes) {
  if (bytes == null) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Today's date as "YYYY-MM-DD" in local time (for <input type="date" min>). */
export function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function errorText(error) {
  return error?.message || 'Something went wrong. Please try again.';
}
