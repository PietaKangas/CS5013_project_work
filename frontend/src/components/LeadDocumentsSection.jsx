import { useState } from 'react';
import { api } from '../api/client';
import { useApi } from '../hooks/useApi';
import Alert from './Alert';
import DocumentChecklistTable from './DocumentChecklistTable';
import ProgressBar from './ProgressBar';
import ReviewDialog from './ReviewDialog';
import Spinner from './Spinner';
import { CATEGORY_LABELS, errorText } from '../utils/format';

/**
 * A student's document checklist as the Lead sees it: open each file, then verify it
 * or send it back with a comment. onChanged is called after each review (to refresh totals).
 */
export default function LeadDocumentsSection({ studentId, onChanged }) {
  const { data, loading, error, reload } = useApi(`/students/${studentId}/documents`);
  const [reviewing, setReviewing] = useState(null); // { item, decision }
  const [busy, setBusy] = useState(false);
  const [dialogError, setDialogError] = useState('');
  const [message, setMessage] = useState('');
  const [actionError, setActionError] = useState('');

  const submitReview = async (decision, comment) => {
    setBusy(true);
    setDialogError('');
    try {
      await api(`/documents/${reviewing.item.document.id}/review`, { method: 'PUT', body: { decision, comment } });
      setMessage(decision === 'VERIFIED'
        ? `${reviewing.item.name} verified.`
        : `${reviewing.item.name} sent back to the student for correction.`);
      setReviewing(null);
      reload();
      onChanged?.();
    } catch (err) {
      setDialogError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const verifyAllWaiting = async () => {
    const waiting = data.items.filter((i) => i.document?.status === 'UPLOADED');
    setActionError('');
    try {
      for (const item of waiting) {
        await api(`/documents/${item.document.id}/review`, { method: 'PUT', body: { decision: 'VERIFIED' } });
      }
      setMessage(`${waiting.length} document${waiting.length === 1 ? '' : 's'} verified.`);
      reload();
      onChanged?.();
    } catch (err) {
      setActionError(err.message);
      reload();
    }
  };

  const waitingCount = data?.items.filter((i) => i.document?.status === 'UPLOADED').length ?? 0;

  return (
    <section className="card">
      <div className="card__header">
        <h2 className="card__title">Documents</h2>
        {waitingCount > 1 && (
          <button type="button" className="btn btn--small btn--ghost" onClick={verifyAllWaiting}>
            Verify all {waitingCount} awaiting review
          </button>
        )}
      </div>
      {loading && !data && <Spinner inline />}
      {error && <Alert>{errorText(error)}</Alert>}
      <Alert type="success" onClose={() => setMessage('')}>{message}</Alert>
      <Alert onClose={() => setActionError('')}>{actionError}</Alert>
      {data && (
        <>
          <ProgressBar percent={data.completionPercent}
            label={`${data.uploadedCount} of ${data.requiredCount} uploaded · ${data.verifiedCount} verified`} />
          <p className="muted small">
            {data.studentCategory ? CATEGORY_LABELS[data.studentCategory] : 'Student category not set yet (only common documents shown)'}
            {' · Open each file and check it before verifying.'}
          </p>
          <DocumentChecklistTable
            items={data.items}
            onReview={(item, decision) => { setDialogError(''); setReviewing({ item, decision }); }}
            onError={setActionError}
          />
        </>
      )}
      <ReviewDialog
        open={reviewing != null}
        title={reviewing?.item.name}
        subtitle={reviewing?.item.document?.originalFilename}
        initialDecision={reviewing?.decision}
        busy={busy}
        error={dialogError}
        onSubmit={submitReview}
        onCancel={() => setReviewing(null)}
      />
    </section>
  );
}
