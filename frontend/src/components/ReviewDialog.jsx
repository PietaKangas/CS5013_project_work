import { useEffect, useRef, useState } from 'react';

/**
 * The Lead's review popup: "Verify" or "Request correction" with a comment the student will see.
 *   <ReviewDialog open title="Passport" initialDecision="NEEDS_CORRECTION"
 *                 onSubmit={(decision, comment) => ...} onCancel={...} busy={...} error={...} />
 */
export default function ReviewDialog({ open, title, subtitle, initialDecision = 'VERIFIED', busy, error, onSubmit, onCancel }) {
  const [decision, setDecision] = useState(initialDecision);
  const [comment, setComment] = useState('');
  const [localError, setLocalError] = useState('');
  const commentRef = useRef(null);

  useEffect(() => {
    if (open) {
      setDecision(initialDecision);
      setComment('');
      setLocalError('');
    }
  }, [open, initialDecision]);

  useEffect(() => {
    if (open && decision === 'NEEDS_CORRECTION') commentRef.current?.focus();
  }, [open, decision]);

  if (!open) return null;

  const submit = (e) => {
    e.preventDefault();
    if (decision === 'NEEDS_CORRECTION' && !comment.trim()) {
      setLocalError('Tell the student what to correct.');
      return;
    }
    onSubmit(decision, comment.trim());
  };

  return (
    <div className="modal-backdrop" onMouseDown={() => !busy && onCancel()}>
      <form
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="review-title"
        onMouseDown={(e) => e.stopPropagation()}
        onSubmit={submit}
        onKeyDown={(e) => { if (e.key === 'Escape' && !busy) onCancel(); }}
      >
        <h2 id="review-title" className="modal__title">Review: {title}</h2>
        {subtitle && <p className="muted small">{subtitle}</p>}

        <fieldset className="decision">
          <legend className="sr-only">Decision</legend>
          <label className={decision === 'VERIFIED' ? 'decision__option decision__option--good is-selected' : 'decision__option decision__option--good'}>
            <input type="radio" name="decision" value="VERIFIED" checked={decision === 'VERIFIED'}
              onChange={() => { setDecision('VERIFIED'); setLocalError(''); }} />
            ✓ Verify — the information is correct
          </label>
          <label className={decision === 'NEEDS_CORRECTION' ? 'decision__option decision__option--bad is-selected' : 'decision__option decision__option--bad'}>
            <input type="radio" name="decision" value="NEEDS_CORRECTION" checked={decision === 'NEEDS_CORRECTION'}
              onChange={() => setDecision('NEEDS_CORRECTION')} />
            Request correction — send it back to the student
          </label>
        </fieldset>

        <div className={localError ? 'field field--error' : 'field'}>
          <label className="field__label" htmlFor="review-comment">
            {decision === 'NEEDS_CORRECTION' ? 'What should the student correct?' : 'Note (optional)'}
          </label>
          <textarea id="review-comment" ref={commentRef} rows={3} maxLength={500} value={comment}
            onChange={(e) => { setComment(e.target.value); setLocalError(''); }}
            placeholder={decision === 'NEEDS_CORRECTION' ? 'e.g. The scan is blurry, please upload a clearer copy.' : ''} />
          {(localError || error) && <p className="field__error">{localError || error}</p>}
          {decision === 'NEEDS_CORRECTION' && !localError && !error && (
            <p className="field__hint">The student sees this message on their dashboard.</p>
          )}
        </div>

        <div className="modal__actions">
          <button type="button" className="btn btn--ghost" onClick={onCancel} disabled={busy}>Cancel</button>
          <button type="submit" className={decision === 'VERIFIED' ? 'btn btn--primary' : 'btn btn--danger'} disabled={busy}>
            {busy ? 'Saving…' : decision === 'VERIFIED' ? 'Mark as verified' : 'Send back for correction'}
          </button>
        </div>
      </form>
    </div>
  );
}
