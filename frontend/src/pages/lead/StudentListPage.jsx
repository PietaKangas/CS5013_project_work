import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import { CompletionBadge, HostelBadge, ReviewStateBadge } from '../../components/Badge';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import Spinner from '../../components/Spinner';
import { HOSTEL_LABELS, REVIEW_STATE_LABELS, errorText } from '../../utils/format';

const PAGE_SIZE = 15;
const SORTABLE = { name: 'Name', studentNumber: 'Roll no.', review: 'Review', profile: 'Profile', documents: 'Documents' };

/**
 * Search, filter and sort are kept in the URL (?search=...&sort=...), so the
 * browser back button and shared links keep the same view.
 */
export default function StudentListPage() {
  const [params, setParams] = useSearchParams();
  const [searchText, setSearchText] = useState(params.get('search') ?? '');

  const sort = params.get('sort') ?? 'name';
  const direction = params.get('direction') ?? 'asc';
  const page = Number(params.get('page') ?? 0);

  const setParam = (key, value) => {
    setParams((prev) => {
      const next = new URLSearchParams(prev);
      if (value === '' || value == null) next.delete(key);
      else next.set(key, value);
      if (key !== 'page') next.delete('page'); // new filter -> back to first page
      return next;
    }, { replace: true });
  };

  // Wait until the user stops typing for 300 ms before searching.
  useEffect(() => {
    const timer = setTimeout(() => {
      if ((params.get('search') ?? '') !== searchText.trim()) setParam('search', searchText.trim());
    }, 300);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchText]);

  const query = new URLSearchParams(params);
  query.set('size', PAGE_SIZE);
  const { data, loading, error } = useApi(`/students?${query.toString()}`);
  const docTypes = useApi('/admin/document-types');

  const toggleSort = (key) => {
    const next = new URLSearchParams(params);
    next.set('sort', key);
    next.set('direction', sort === key && direction === 'asc' ? 'desc' : 'asc');
    next.delete('page');
    setParams(next, { replace: true });
  };

  const sortHeader = (key) => (
    <th scope="col" aria-sort={sort === key ? (direction === 'asc' ? 'ascending' : 'descending') : 'none'}>
      <button type="button" className="sort-button" onClick={() => toggleSort(key)}>
        {SORTABLE[key]}
        <span aria-hidden="true">{sort === key ? (direction === 'asc' ? ' ▲' : ' ▼') : ''}</span>
      </button>
    </th>
  );

  const hasFilters = ['search', 'reviewState', 'profileComplete', 'documentsComplete', 'hostelStatus', 'missingDocumentTypeId']
    .some((k) => params.get(k));

  return (
    <>
      <PageHeader title="Students" subtitle="Every international student in one place. Open a student to review and verify their details." />

      <div className="card filters">
        <div className="filters__search">
          <label htmlFor="search" className="sr-only">Search</label>
          <input id="search" type="search" placeholder="Search name, email, roll no., university, room…"
            value={searchText} onChange={(e) => setSearchText(e.target.value)} />
        </div>
        <select aria-label="Review status" value={params.get('reviewState') ?? ''}
          onChange={(e) => setParam('reviewState', e.target.value)}>
          <option value="">Review: any</option>
          {Object.entries(REVIEW_STATE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>Review: {label}</option>
          ))}
        </select>
        <select aria-label="Profile completion" value={params.get('profileComplete') ?? ''}
          onChange={(e) => setParam('profileComplete', e.target.value)}>
          <option value="">Profile: any</option>
          <option value="true">Profile complete</option>
          <option value="false">Profile incomplete</option>
        </select>
        <select aria-label="Document completion" value={params.get('documentsComplete') ?? ''}
          onChange={(e) => setParam('documentsComplete', e.target.value)}>
          <option value="">Documents: any</option>
          <option value="true">All documents submitted</option>
          <option value="false">Documents missing</option>
        </select>
        <select aria-label="Missing document" value={params.get('missingDocumentTypeId') ?? ''}
          onChange={(e) => setParam('missingDocumentTypeId', e.target.value)}>
          <option value="">Missing: any document</option>
          {docTypes.data?.filter((t) => t.active).map((t) => (
            <option key={t.id} value={t.id}>Missing: {t.name}</option>
          ))}
        </select>
        <select aria-label="Hostel status" value={params.get('hostelStatus') ?? ''}
          onChange={(e) => setParam('hostelStatus', e.target.value)}>
          <option value="">Hostel: any</option>
          {Object.entries(HOSTEL_LABELS).map(([value, label]) => (
            <option key={value} value={value}>Hostel: {label}</option>
          ))}
        </select>
        {hasFilters && (
          <button type="button" className="btn btn--ghost btn--small" onClick={() => {
            setSearchText('');
            setParams({}, { replace: true });
          }}>
            Clear filters
          </button>
        )}
      </div>

      {error && <Alert>{errorText(error)}</Alert>}
      {loading && !data && <Spinner />}

      {data && (
        <>
          <p className="muted small results-count">
            {data.totalElements} student{data.totalElements === 1 ? '' : 's'} found
            {loading && ' · updating…'}
          </p>
          {data.content.length === 0 ? (
            <EmptyState title="No students match these filters.">
              {hasFilters ? 'Try clearing the filters.' : 'Students appear here after they sign up.'}
            </EmptyState>
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    {sortHeader('name')}
                    {sortHeader('studentNumber')}
                    <th scope="col">Home university</th>
                    {sortHeader('review')}
                    {sortHeader('profile')}
                    {sortHeader('documents')}
                    <th scope="col">Hostel</th>
                    <th scope="col" className="table__actions-col">Open</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((s) => (
                    <tr key={s.id}>
                      <td>
                        <Link to={`/lead/students/${s.id}`}><strong>{s.fullName}</strong></Link>
                        <div className="muted small">{s.email}</div>
                      </td>
                      <td>{s.studentNumber ?? <span className="muted">—</span>}</td>
                      <td>
                        {s.homeUniversity ?? <span className="muted">—</span>}
                        {s.department && <div className="muted small">{s.department}</div>}
                      </td>
                      <td>
                        <ReviewStateBadge state={s.reviewState} />
                        {s.documentsAwaitingReview > 0 && (
                          <div className="muted small">{s.documentsAwaitingReview} doc{s.documentsAwaitingReview === 1 ? '' : 's'} to review</div>
                        )}
                        {s.documentsNeedingCorrection.length > 0 && (
                          <div className="muted small">Fix: {s.documentsNeedingCorrection.join(', ')}</div>
                        )}
                      </td>
                      <td><CompletionBadge complete={s.profileComplete} percent={s.profileCompletionPercent} /></td>
                      <td>
                        <CompletionBadge complete={s.documentsComplete} percent={s.documentCompletionPercent} />
                        <div className="muted small">{s.documentsUploaded}/{s.documentsRequired} uploaded · {s.documentsVerified} verified</div>
                      </td>
                      <td>
                        <HostelBadge status={s.hostelStatus} />
                        {s.roomNumber && <div className="muted small">{s.hostelName?.replace(' Hostel', '')} {s.roomNumber}</div>}
                      </td>
                      <td>
                        <div className="row-actions">
                          <Link className="btn btn--small btn--primary" to={`/lead/students/${s.id}`}>Review</Link>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {data.totalPages > 1 && (
            <nav className="pagination" aria-label="Pages">
              <button type="button" className="btn btn--small btn--ghost" disabled={page <= 0}
                onClick={() => setParam('page', page - 1)}>
                ← Previous
              </button>
              <span>Page {page + 1} of {data.totalPages}</span>
              <button type="button" className="btn btn--small btn--ghost" disabled={page + 1 >= data.totalPages}
                onClick={() => setParam('page', page + 1)}>
                Next →
              </button>
            </nav>
          )}
        </>
      )}
    </>
  );
}
