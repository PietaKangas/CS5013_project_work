import { useRef } from 'react';
import { DocumentStatusBadge } from './Badge';
import EmptyState from './EmptyState';
import { formatBytes, formatDateTime } from '../utils/format';
import { downloadDocument, viewDocument } from '../utils/files';

/**
 * The required-documents checklist, shared by both sides:
 *  - student (editable):   upload / replace / delete, sees the Lead's correction comments
 *  - Lead (onReview set):  view / download and Verify / Request correction for each document
 */
export default function DocumentChecklistTable({
  items,
  editable = false,
  busyTypeId = null,
  onUpload,
  onDelete,
  onReview,
  onError,
}) {
  if (!items?.length) {
    return <EmptyState title="No documents are required at the moment." />;
  }

  const run = async (action) => {
    try {
      await action();
    } catch (err) {
      onError?.(err.message);
    }
  };

  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th scope="col">Document</th>
            <th scope="col">Status</th>
            <th scope="col">File</th>
            <th scope="col" className="table__actions-col">Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => {
            const doc = item.document;
            const busy = busyTypeId === item.documentTypeId;
            const needsFix = doc?.status === 'NEEDS_CORRECTION';
            return (
              <tr key={item.documentTypeId} className={needsFix ? 'row--attention' : undefined}>
                <td>
                  <strong>{item.name}</strong>
                  {item.description && <div className="muted small">{item.description}</div>}
                  {item.formUrl && (
                    <a className="small form-link" href={item.formUrl} target="_blank" rel="noreferrer">
                      Open the form ↗
                    </a>
                  )}
                </td>
                <td>
                  <DocumentStatusBadge document={doc} />
                  {doc?.reviewComment && (
                    <div className={needsFix ? 'review-comment review-comment--bad' : 'review-comment'}>
                      {doc.reviewComment}
                    </div>
                  )}
                </td>
                <td>
                  {doc ? (
                    <>
                      <div className="filename" title={doc.originalFilename}>{doc.originalFilename}</div>
                      <div className="muted small">
                        {formatBytes(doc.sizeBytes)} · uploaded {formatDateTime(doc.uploadedAt)}
                      </div>
                    </>
                  ) : (
                    <span className="muted">—</span>
                  )}
                </td>
                <td>
                  <div className="row-actions">
                    {doc && (
                      <>
                        <button type="button" className="btn btn--small btn--ghost"
                          onClick={() => run(() => viewDocument(doc.id))}>
                          View
                        </button>
                        <button type="button" className="btn btn--small btn--ghost"
                          onClick={() => run(() => downloadDocument(doc.id, doc.originalFilename))}>
                          Download
                        </button>
                      </>
                    )}
                    {onReview && doc && (
                      <>
                        {doc.status !== 'VERIFIED' && (
                          <button type="button" className="btn btn--small btn--primary"
                            onClick={() => onReview(item, 'VERIFIED')}>
                            Verify
                          </button>
                        )}
                        <button type="button" className="btn btn--small btn--danger-ghost"
                          onClick={() => onReview(item, 'NEEDS_CORRECTION')}>
                          {doc.status === 'NEEDS_CORRECTION' ? 'Edit request' : 'Request correction'}
                        </button>
                      </>
                    )}
                    {editable && (
                      <UploadButton
                        label={doc ? (needsFix ? 'Upload corrected file' : 'Replace') : 'Upload'}
                        primary={!doc || needsFix}
                        busy={busy}
                        onFile={(file) => onUpload(item.documentTypeId, file)}
                      />
                    )}
                    {editable && doc && (
                      <button type="button" className="btn btn--small btn--danger-ghost"
                        disabled={busy} onClick={() => onDelete(item)}>
                        Delete
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

/** A button that opens the file picker (the real <input type="file"> is hidden). */
function UploadButton({ label, busy, primary, onFile }) {
  const inputRef = useRef(null);
  return (
    <>
      <input
        ref={inputRef}
        type="file"
        accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
        hidden
        onChange={(e) => {
          const file = e.target.files?.[0];
          e.target.value = ''; // allow choosing the same file again later
          if (file) onFile(file);
        }}
      />
      <button type="button" className={primary ? 'btn btn--small btn--primary' : 'btn btn--small btn--ghost'}
        disabled={busy} onClick={() => inputRef.current?.click()}>
        {busy ? 'Uploading…' : label}
      </button>
    </>
  );
}
