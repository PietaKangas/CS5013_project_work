import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../api/client';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import Badge, { HostelBadge, ReviewBadge, ReviewStateBadge } from '../../components/Badge';
import Field from '../../components/Field';
import LeadDocumentsSection from '../../components/LeadDocumentsSection';
import PageHeader from '../../components/PageHeader';
import ProgressBar from '../../components/ProgressBar';
import ReviewDialog from '../../components/ReviewDialog';
import ReviewNotice from '../../components/ReviewNotice';
import Spinner from '../../components/Spinner';
import {
  CATEGORY_LABELS, GENDER_LABELS, HOSTEL_LABELS, errorText, formatDate, formatDateTime, formatTime,
} from '../../utils/format';
import { HOSTELS } from '../../config/institution';

function Detail({ label, children }) {
  return (
    <div className="detail">
      <dt>{label}</dt>
      <dd>{children || <span className="muted">Not provided</span>}</dd>
    </div>
  );
}

/**
 * Everything about one student on one page, for the International Lead:
 * profile, hostel, documents and appointments, each with Verify / Request correction.
 * Loaded with the Lead's own login — no student credentials are needed.
 */
export default function StudentProfileView() {
  const { id } = useParams();
  const profile = useApi(`/students/${id}/profile`);
  const summary = useApi(`/students/${id}`);
  const bookings = useApi('/appointments/bookings');

  const [reviewing, setReviewing] = useState(null); // { section: 'profile' | 'hostel', decision }
  const [busy, setBusy] = useState(false);
  const [dialogError, setDialogError] = useState('');
  const [message, setMessage] = useState('');

  const p = profile.data;
  const refresh = () => {
    profile.reload();
    summary.reload();
  };

  const submitReview = async (decision, comment) => {
    setBusy(true);
    setDialogError('');
    try {
      await api(`/students/${id}/${reviewing.section}/review`, { method: 'PUT', body: { decision, comment } });
      const what = reviewing.section === 'profile' ? 'Profile' : 'Hostel details';
      setMessage(decision === 'VERIFIED' ? `${what} verified.` : `${what} sent back to the student for correction.`);
      setReviewing(null);
      refresh();
    } catch (err) {
      setDialogError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const studentBookings = bookings.data?.filter((b) => String(b.studentId) === String(id)) ?? [];

  return (
    <>
      <Link className="back-link" to="/lead/students">← All students</Link>
      {profile.loading && !p && <Spinner />}
      {profile.error && <Alert>{errorText(profile.error)}</Alert>}

      {p && (
        <>
          <PageHeader
            title={p.fullName}
            subtitle={[p.email, p.studentNumber, p.homeUniversity].filter(Boolean).join(' · ')}
            actions={summary.data && <ReviewStateBadge state={summary.data.reviewState} />}
          />
          <Alert type="success" onClose={() => setMessage('')}>{message}</Alert>

          {/* ---------------- profile ---------------- */}
          <section className="card">
            <div className="card__header">
              <h2 className="card__title">Profile <ReviewBadge status={p.profileStatus} /></h2>
              <div className="row-actions">
                {p.profileStatus !== 'VERIFIED' && (
                  <button type="button" className="btn btn--small btn--primary" disabled={!p.profileComplete}
                    title={p.profileComplete ? '' : 'The profile is incomplete'}
                    onClick={() => { setDialogError(''); setReviewing({ section: 'profile', decision: 'VERIFIED' }); }}>
                    Verify profile
                  </button>
                )}
                <button type="button" className="btn btn--small btn--danger-ghost"
                  onClick={() => { setDialogError(''); setReviewing({ section: 'profile', decision: 'NEEDS_CORRECTION' }); }}>
                  Request correction
                </button>
              </div>
            </div>
            <ProgressBar percent={p.profileCompletionPercent} label={p.profileComplete ? 'All fields filled in' : 'Incomplete'} />
            {!p.profileComplete && <p className="muted small">Missing: {p.missingFields.join(', ')}</p>}
            <ReviewNotice status={p.profileStatus} comment={p.profileComment} reviewedAt={p.profileReviewedAt}
              pendingText={p.profileComplete ? 'Check the details against the documents below.' : 'The student hasn’t finished the profile yet.'} />

            <h3 className="info__heading">Personal</h3>
            <dl className="details">
              <Detail label="Full name">{p.fullName}</Detail>
              <Detail label="Email">{p.email}</Detail>
              <Detail label="Gender">{GENDER_LABELS[p.gender]}</Detail>
              <Detail label="Date of birth">{p.dateOfBirth && formatDate(p.dateOfBirth)}</Detail>
              <Detail label="Nationality">{p.nationality}</Detail>
              <Detail label="Phone">{p.phone}</Detail>
              <Detail label="Address in India">{p.address}</Detail>
              <Detail label="Emergency contact">{p.emergencyContact}</Detail>
            </dl>
            <h3 className="info__heading">Passport, visa and travel</h3>
            <dl className="details">
              <Detail label="Passport number">{p.passportNumber}</Detail>
              <Detail label="Passport expiry">{p.passportExpiry && formatDate(p.passportExpiry)}</Detail>
              <Detail label="Visa valid until">{p.visaExpiry && formatDate(p.visaExpiry)}</Detail>
              <Detail label="Arrival">{p.arrivalDate && formatDate(p.arrivalDate)}</Detail>
              <Detail label="Departure">{p.departureDate && formatDate(p.departureDate)}</Detail>
            </dl>
            <h3 className="info__heading">Academic</h3>
            <dl className="details">
              <Detail label="Home university">{p.homeUniversity}</Detail>
              <Detail label="Category">{CATEGORY_LABELS[p.studentCategory]}</Detail>
              <Detail label="Roll number">{p.studentNumber}</Detail>
              <Detail label="Programme">{p.programme}</Detail>
              <Detail label="Department">{p.department}</Detail>
              <Detail label="Semester">{p.academicYear}</Detail>
            </dl>
            <p className="muted small">Last updated by the student: {formatDateTime(p.updatedAt)}</p>
          </section>

          {/* ---------------- hostel ---------------- */}
          <HostelSection studentId={id} profile={p} onChanged={refresh} setMessage={setMessage}
            onReview={(decision) => { setDialogError(''); setReviewing({ section: 'hostel', decision }); }} />

          {/* ---------------- documents ---------------- */}
          <LeadDocumentsSection studentId={id} onChanged={summary.reload} />

          {/* ---------------- appointments ---------------- */}
          <section className="card">
            <h2 className="card__title">Appointments</h2>
            {bookings.loading && !bookings.data && <Spinner inline />}
            {bookings.data && studentBookings.length === 0 && (
              <p className="muted">No appointments with you.</p>
            )}
            {studentBookings.length > 0 && (
              <ul className="list">
                {studentBookings.map((b) => (
                  <li key={b.id} className="list__item">
                    <span>{formatDate(b.date)}, {formatTime(b.startTime)}–{formatTime(b.endTime)}</span>
                    {b.status === 'CANCELLED'
                      ? <Badge tone="bad">Cancelled</Badge>
                      : <Badge tone={b.upcoming ? 'info' : 'neutral'}>{b.upcoming ? 'Booked' : 'Completed'}</Badge>}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <ReviewDialog
            open={reviewing != null}
            title={reviewing?.section === 'profile' ? `${p.fullName} — profile` : `${p.fullName} — hostel details`}
            initialDecision={reviewing?.decision}
            busy={busy}
            error={dialogError}
            onSubmit={submitReview}
            onCancel={() => setReviewing(null)}
          />
        </>
      )}
    </>
  );
}

/** Hostel details with Verify / Request correction, plus an inline form for the Lead to fix them directly. */
function HostelSection({ studentId, profile: p, onChanged, onReview, setMessage }) {
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ hostelStatus: p.hostelStatus, hostelName: p.hostelName ?? '', roomNumber: p.roomNumber ?? '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!editing) setForm({ hostelStatus: p.hostelStatus, hostelName: p.hostelName ?? '', roomNumber: p.roomNumber ?? '' });
  }, [p, editing]);

  const save = async (e) => {
    e.preventDefault();
    if (form.hostelStatus === 'ALLOCATED' && (!form.hostelName || !form.roomNumber.trim())) {
      setError('Choose the hostel and enter the room number.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      await api(`/students/${studentId}/hostel`, { method: 'PUT', body: form });
      setMessage('Hostel details updated and marked as verified.');
      setEditing(false);
      onChanged();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const names = HOSTELS.map((h) => h.name);
  if (form.hostelName && !names.includes(form.hostelName)) names.push(form.hostelName);

  return (
    <section className="card">
      <div className="card__header">
        <h2 className="card__title">Hostel {p.hostelStatus !== 'NOT_APPLIED' && <ReviewBadge status={p.hostelReviewStatus} />}</h2>
        {!editing && (
          <div className="row-actions">
            {p.hostelStatus !== 'NOT_APPLIED' && p.hostelReviewStatus !== 'VERIFIED' && (
              <button type="button" className="btn btn--small btn--primary" onClick={() => onReview('VERIFIED')}>Verify hostel</button>
            )}
            {p.hostelStatus !== 'NOT_APPLIED' && (
              <button type="button" className="btn btn--small btn--danger-ghost" onClick={() => onReview('NEEDS_CORRECTION')}>Request correction</button>
            )}
            <button type="button" className="btn btn--small btn--ghost" onClick={() => setEditing(true)}>Edit</button>
          </div>
        )}
      </div>

      {!editing ? (
        <>
          <dl className="details">
            <Detail label="Status"><HostelBadge status={p.hostelStatus} /></Detail>
            <Detail label="Hostel">{p.hostelName}</Detail>
            <Detail label="Room">{p.roomNumber}</Detail>
          </dl>
          {p.hostelStatus !== 'NOT_APPLIED' && (
            <ReviewNotice status={p.hostelReviewStatus} comment={p.hostelComment} reviewedAt={p.hostelReviewedAt}
              pendingText="Confirm the room with the hostel office’s allotment list." />
          )}
        </>
      ) : (
        <form className="form" onSubmit={save} noValidate>
          {error && <Alert>{error}</Alert>}
          <div className="form__grid form__grid--3">
            <Field label="Allocation status" htmlFor="lead-hostel-status">
              <select id="lead-hostel-status" value={form.hostelStatus} onChange={(e) => setForm({ ...form, hostelStatus: e.target.value })}>
                {Object.entries(HOSTEL_LABELS).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </select>
            </Field>
            <Field label="Hostel" htmlFor="lead-hostel-name">
              <select id="lead-hostel-name" value={form.hostelName} onChange={(e) => setForm({ ...form, hostelName: e.target.value })}>
                <option value="">Choose…</option>
                {names.map((n) => <option key={n} value={n}>{n}</option>)}
              </select>
            </Field>
            <Field label="Room number" htmlFor="lead-room">
              <input id="lead-room" value={form.roomNumber} onChange={(e) => setForm({ ...form, roomNumber: e.target.value })} />
            </Field>
          </div>
          <p className="muted small">Saving here updates the student’s record and marks the hostel details as verified.</p>
          <div className="form__actions">
            <button type="button" className="btn btn--ghost" onClick={() => setEditing(false)} disabled={saving}>Cancel</button>
            <button type="submit" className="btn btn--primary" disabled={saving}>{saving ? 'Saving…' : 'Save and verify'}</button>
          </div>
        </form>
      )}
    </section>
  );
}
