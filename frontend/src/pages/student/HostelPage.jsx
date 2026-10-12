import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import { HostelBadge } from '../../components/Badge';
import Field from '../../components/Field';
import PageHeader from '../../components/PageHeader';
import ReviewNotice from '../../components/ReviewNotice';
import Spinner from '../../components/Spinner';
import { HOSTEL_LABELS, errorText } from '../../utils/format';
import { FEE_OFFICE, HOSTELS, HOSTEL_OFFICE_HOURS, INSTITUTION, LINKS } from '../../config/institution';

export default function HostelPage() {
  const { user } = useAuth();
  const { data: profile, loading, error: loadError } = useApi('/students/me/profile');

  const [form, setForm] = useState(null);
  const [current, setCurrent] = useState(null);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (profile) {
      setCurrent(profile);
      setForm({
        hostelStatus: profile.hostelStatus,
        hostelName: profile.hostelName ?? '',
        roomNumber: profile.roomNumber ?? '',
      });
    }
  }, [profile]);

  if (loading) return <Spinner />;
  if (loadError) return <Alert>{errorText(loadError)}</Alert>;
  if (!form) return null;

  // Keep an older, non-standard value selectable so it isn't silently lost.
  const hostelNames = HOSTELS.map((h) => h.name);
  if (form.hostelName && !hostelNames.includes(form.hostelName)) hostelNames.push(form.hostelName);

  const onChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setSuccess('');
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const newErrors = {};
    if (form.hostelStatus === 'ALLOCATED') {
      if (!form.hostelName.trim()) newErrors.hostelName = 'Choose your hostel';
      if (!form.roomNumber.trim()) newErrors.roomNumber = 'Enter the room number you were allotted';
    }
    setErrors(newErrors);
    if (Object.keys(newErrors).length) return;

    setSaving(true);
    setError('');
    try {
      const updated = await api(`/students/${user.id}/hostel`, { method: 'PUT', body: form });
      setCurrent(updated);
      setSuccess(updated.hostelReviewStatus === 'PENDING'
        ? 'Hostel information saved. The International Office will confirm it.'
        : 'Hostel information saved.');
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Hostel information"
        subtitle={`${INSTITUTION.name} hostel allotment for the ${INSTITUTION.programme}, ${INSTITUTION.semester}.`}
      />

      <div className="card summary-row">
        <span>Current status:</span> <HostelBadge status={current.hostelStatus} />
        {current.hostelStatus === 'ALLOCATED' && (
          <span>{current.hostelName}, room {current.roomNumber}</span>
        )}
      </div>
      {current.hostelStatus !== 'NOT_APPLIED' && (
        <ReviewNotice status={current.hostelReviewStatus} comment={current.hostelComment}
          reviewedAt={current.hostelReviewedAt}
          pendingText="The International Office will confirm your hostel and room details." />
      )}

      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>

      <form className="card form" onSubmit={onSubmit} noValidate>
        <h2 className="card__title">My allotment</h2>
        <div className="form__grid">
          <Field label="Allocation status" htmlFor="hostelStatus" error={errors.hostelStatus} required
            hint="Choose “Allocated” once the hostel office has given you a room.">
            <select id="hostelStatus" name="hostelStatus" value={form.hostelStatus} onChange={onChange}>
              {Object.entries(HOSTEL_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </Field>
          <Field label="Hostel" htmlFor="hostelName" error={errors.hostelName}
            required={form.hostelStatus === 'ALLOCATED'}>
            <select id="hostelName" name="hostelName" value={form.hostelName} onChange={onChange}>
              <option value="">Choose…</option>
              {hostelNames.map((name) => {
                const info = HOSTELS.find((h) => h.name === name);
                return (
                  <option key={name} value={name}>{info ? `${name} (${info.forWhom.toLowerCase()})` : name}</option>
                );
              })}
            </select>
          </Field>
          <Field label="Room number" htmlFor="roomNumber" error={errors.roomNumber}
            required={form.hostelStatus === 'ALLOCATED'}>
            <input id="roomNumber" name="roomNumber" value={form.roomNumber} onChange={onChange} />
          </Field>
        </div>
        <div className="form__actions">
          <button type="submit" className="btn btn--primary" disabled={saving}>
            {saving ? 'Saving…' : 'Save hostel information'}
          </button>
        </div>
      </form>

      <section className="card">
        <h2 className="card__title">Hostel allotment</h2>
        <div className="grid grid--2 grid--flat">
          <div>
            <h3 className="info__heading">Hostels</h3>
            <ul className="info__list">
              {HOSTELS.map((h) => (
                <li key={h.name}><strong>{h.name}</strong> — {h.forWhom}</li>
              ))}
            </ul>
          </div>
          <div>
            <h3 className="info__heading">Hostel office timings</h3>
            <ul className="info__list">
              {HOSTEL_OFFICE_HOURS.map((t) => (
                <li key={t.days}><strong>{t.days}:</strong> {t.hours}</li>
              ))}
            </ul>
          </div>
        </div>

        <h3 className="info__heading">Room allotment process</h3>
        <ol className="info__steps">
          <li>
            <strong>Before arrival:</strong> fill in the online{' '}
            <a href={LINKS.biodataForm} target="_blank" rel="noreferrer">BIODATA form</a>, paste a recent colour
            passport-size photograph on it and bring a signed hard copy, together with the declaration forms.
          </li>
          <li>
            <strong>On arrival:</strong> report to your hostel office (Tamiraparani / Sabarmati) and hand the Hostel
            Manager your IIT Madras offer letter, the signed BIODATA form, the{' '}
            <a href={LINKS.selfDeclaration} target="_blank" rel="noreferrer">Self-Declaration Addendum</a> and the{' '}
            <a href={LINKS.solemnAffirmation} target="_blank" rel="noreferrer">
              Solemn Affirmation of Conformance to Rules and Regulations
            </a>.
          </li>
          <li><strong>After room allotment:</strong> go to your room and keep your luggage, then update your allotment above.</li>
          <li>
            <strong>Hostel fee payment:</strong> {FEE_OFFICE.name}, {FEE_OFFICE.room} (
            <a href={LINKS.hostelManagementMap} target="_blank" rel="noreferrer">map</a>).
          </li>
        </ol>
        <p className="muted small">
          Upload scans of all these documents on the <Link to="/student/documents">Documents</Link> page as well.
        </p>
      </section>
    </>
  );
}
