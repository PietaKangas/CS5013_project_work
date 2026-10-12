import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import Field from '../../components/Field';
import PageHeader from '../../components/PageHeader';
import ProgressBar from '../../components/ProgressBar';
import ReviewNotice from '../../components/ReviewNotice';
import Spinner from '../../components/Spinner';
import { CATEGORY_LABELS, GENDER_LABELS, errorText } from '../../utils/format';
import { INSTITUTION } from '../../config/institution';

// Every editable field. Dates, gender and category are sent as null when empty.
const FIELDS = [
  'fullName', 'phone', 'dateOfBirth', 'gender', 'nationality', 'address', 'emergencyContact',
  'passportNumber', 'passportExpiry', 'visaExpiry', 'arrivalDate', 'departureDate',
  'homeUniversity', 'studentNumber', 'programme', 'department', 'academicYear', 'studentCategory',
];
const NULL_WHEN_EMPTY = ['dateOfBirth', 'gender', 'passportExpiry', 'visaExpiry', 'arrivalDate', 'departureDate', 'studentCategory'];

function toForm(profile) {
  const form = {};
  FIELDS.forEach((key) => { form[key] = profile[key] ?? ''; });
  return form;
}

export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const { data: profile, loading, error: loadError } = useApi('/students/me/profile');

  const [form, setForm] = useState(null);
  const [saved, setSaved] = useState(null); // latest profile returned by the server
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (profile) {
      setForm(toForm(profile));
      setSaved(profile);
    }
  }, [profile]);

  if (loading) return <Spinner />;
  if (loadError) return <Alert>{errorText(loadError)}</Alert>;
  if (!form) return null;

  const onChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setSuccess('');
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!form.fullName.trim()) {
      setErrors({ fullName: 'Full name is required' });
      return;
    }
    setSaving(true);
    setErrors({});
    setError('');
    try {
      const body = { ...form };
      NULL_WHEN_EMPTY.forEach((key) => { if (!body[key]) body[key] = null; });
      const wasVerified = saved.profileStatus === 'VERIFIED';
      const updated = await api(`/students/${user.id}/profile`, { method: 'PUT', body });
      setSaved(updated);
      setForm(toForm(updated));
      updateUser({ fullName: updated.fullName });
      setSuccess(wasVerified && updated.profileStatus === 'PENDING'
        ? 'Profile saved. Because you changed verified details, the International Office will review them again.'
        : 'Profile saved.');
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const input = (name, props = {}) => (
    <input id={name} name={name} value={form[name]} onChange={onChange} {...props} />
  );
  const select = (name, labels) => (
    <select id={name} name={name} value={form[name]} onChange={onChange}>
      <option value="">Choose…</option>
      {Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
    </select>
  );

  return (
    <>
      <PageHeader title="My profile" subtitle="Keep your personal, passport and academic details up to date." />

      <div className="card">
        <ProgressBar percent={saved.profileCompletionPercent} label="Profile completion" />
        {!saved.profileComplete && (
          <p className="muted small">Still missing: {saved.missingFields.join(', ')}</p>
        )}
        {saved.profileComplete || saved.profileStatus !== 'PENDING' ? (
          <ReviewNotice status={saved.profileStatus} comment={saved.profileComment} reviewedAt={saved.profileReviewedAt}
            pendingText="The International Office will check your details soon." />
        ) : (
          <p className="muted small">Complete all fields so the International Office can verify your profile.</p>
        )}
      </div>

      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>

      <form className="card form" onSubmit={onSubmit} noValidate>
        <h2 className="form__section">Personal information</h2>
        <div className="form__grid">
          <Field label="Full name (as in passport)" htmlFor="fullName" error={errors.fullName} required>
            {input('fullName', { autoComplete: 'name' })}
          </Field>
          <Field label="Email" htmlFor="email" hint="Your login email can't be changed here.">
            <input id="email" value={saved.email} disabled />
          </Field>
          <Field label="Gender" htmlFor="gender" error={errors.gender} hint="Used by the hostel office for allotment.">
            {select('gender', GENDER_LABELS)}
          </Field>
          <Field label="Date of birth" htmlFor="dateOfBirth" error={errors.dateOfBirth}>
            {input('dateOfBirth', { type: 'date' })}
          </Field>
          <Field label="Nationality" htmlFor="nationality" error={errors.nationality}>
            {input('nationality', { placeholder: 'e.g. Finnish' })}
          </Field>
          <Field label="Phone number (Indian number if you have one)" htmlFor="phone" error={errors.phone}>
            {input('phone', { type: 'tel', autoComplete: 'tel', placeholder: '+91 90000 00000' })}
          </Field>
          <Field label="Address in India" htmlFor="address" error={errors.address}>
            <textarea id="address" name="address" rows={2} value={form.address} onChange={onChange} />
          </Field>
          <Field label="Emergency contact (optional)" htmlFor="emergencyContact" error={errors.emergencyContact}
            hint="Name, relationship and phone number">
            {input('emergencyContact')}
          </Field>
        </div>

        <h2 className="form__section">Passport, visa and travel</h2>
        <div className="form__grid">
          <Field label="Passport number" htmlFor="passportNumber" error={errors.passportNumber}>
            {input('passportNumber', { autoComplete: 'off' })}
          </Field>
          <Field label="Passport expiry date" htmlFor="passportExpiry" error={errors.passportExpiry}>
            {input('passportExpiry', { type: 'date' })}
          </Field>
          <Field label="Visa valid until (optional)" htmlFor="visaExpiry" error={errors.visaExpiry}>
            {input('visaExpiry', { type: 'date' })}
          </Field>
          <Field label="Arrival date in India" htmlFor="arrivalDate" error={errors.arrivalDate}>
            {input('arrivalDate', { type: 'date' })}
          </Field>
          <Field label="Planned departure date (optional)" htmlFor="departureDate" error={errors.departureDate}>
            {input('departureDate', { type: 'date' })}
          </Field>
        </div>

        <h2 className="form__section">Academic information</h2>
        <div className="form__grid">
          <Field label="Home university" htmlFor="homeUniversity" error={errors.homeUniversity}>
            {input('homeUniversity', { placeholder: 'e.g. Aalto University' })}
          </Field>
          <Field label="Student category" htmlFor="studentCategory" error={errors.studentCategory}
            hint="Decides which documents you need to upload.">
            {select('studentCategory', CATEGORY_LABELS)}
          </Field>
          <Field label={`${INSTITUTION.name} roll number`} htmlFor="studentNumber" error={errors.studentNumber}>
            {input('studentNumber', { placeholder: 'As on your offer letter' })}
          </Field>
          <Field label="Programme" htmlFor="programme" error={errors.programme}>
            {input('programme', { placeholder: 'e.g. Semester exchange – B.Tech' })}
          </Field>
          <Field label="Host department at IIT Madras" htmlFor="department" error={errors.department}>
            {input('department', { placeholder: 'e.g. Computer Science and Engineering' })}
          </Field>
          <Field label="Semester" htmlFor="academicYear" error={errors.academicYear}>
            {input('academicYear', { placeholder: `e.g. ${INSTITUTION.semester}` })}
          </Field>
        </div>

        <div className="form__actions">
          <button type="submit" className="btn btn--primary" disabled={saving}>
            {saving ? 'Saving…' : 'Save profile'}
          </button>
        </div>
      </form>
    </>
  );
}
