import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import ConfirmDialog from '../../components/ConfirmDialog';
import DocumentChecklistTable from '../../components/DocumentChecklistTable';
import PageHeader from '../../components/PageHeader';
import ProgressBar from '../../components/ProgressBar';
import Spinner from '../../components/Spinner';
import { errorText } from '../../utils/format';
import { checkFile } from '../../utils/files';

export default function DocumentsPage() {
  const { data: checklist, loading, error: loadError, reload } = useApi('/documents/required');

  const [busyTypeId, setBusyTypeId] = useState(null);
  const [toDelete, setToDelete] = useState(null); // checklist item waiting for confirmation
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const upload = async (documentTypeId, file) => {
    setError('');
    setSuccess('');
    const problem = checkFile(file);
    if (problem) {
      setError(problem);
      return;
    }
    const formData = new FormData();
    formData.append('documentTypeId', documentTypeId);
    formData.append('file', file);

    setBusyTypeId(documentTypeId);
    try {
      const doc = await api('/documents', { method: 'POST', formData });
      setSuccess(`"${doc.originalFilename}" uploaded for ${doc.documentTypeName}. It will be reviewed by the International Office.`);
      reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyTypeId(null);
    }
  };

  const confirmDelete = async () => {
    setDeleting(true);
    setError('');
    try {
      await api(`/documents/${toDelete.document.id}`, { method: 'DELETE' });
      setSuccess(`${toDelete.name} deleted.`);
      setToDelete(null);
      reload();
    } catch (err) {
      setError(err.message);
      setToDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="My documents"
        subtitle="Upload a scan of each document below as PDF, JPG or PNG (max 5 MB). Uploading again replaces the file. Bring the signed originals and hard copies when you report to your hostel office."
      />

      {loading && !checklist && <Spinner />}
      {loadError && <Alert>{errorText(loadError)}</Alert>}
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>

      {checklist && (
        <>
          <div className="card">
            <ProgressBar
              percent={checklist.completionPercent}
              label={`${checklist.uploadedCount} of ${checklist.requiredCount} required documents uploaded`}
            />
            <p className="muted small">
              {checklist.verifiedCount} of {checklist.requiredCount} verified by the International Office
              {checklist.needsCorrectionCount > 0 && (
                <strong className="bad-text"> · {checklist.needsCorrectionCount} need{checklist.needsCorrectionCount === 1 ? 's' : ''} correction — see the red rows below</strong>
              )}
            </p>
            {!checklist.studentCategory && (
              <p className="muted small">
                Some documents depend on whether you are a semester exchange or full-time degree student.{' '}
                <Link to="/student/profile">Set your student category</Link> to see the full list.
              </p>
            )}
          </div>

          <DocumentChecklistTable
            items={checklist.items}
            editable
            busyTypeId={busyTypeId}
            onUpload={upload}
            onDelete={setToDelete}
            onError={setError}
          />
        </>
      )}

      <ConfirmDialog
        open={toDelete != null}
        title="Delete this document?"
        message={toDelete && (
          <>
            <p>
              <strong>{toDelete.document.originalFilename}</strong> ({toDelete.name}) will be permanently deleted.
            </p>
            <p className="muted">It will show as missing until you upload it again.</p>
          </>
        )}
        confirmLabel="Delete"
        danger
        busy={deleting}
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
