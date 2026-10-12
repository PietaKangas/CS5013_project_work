import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { homeFor, useAuth } from '../auth/AuthContext';
import Alert from '../components/Alert';
import Field from '../components/Field';
import { APP_TITLE, INSTITUTION } from '../config/institution';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateAccountForm(form) {
  const errors = {};
  if (!form.fullName.trim()) errors.fullName = 'Full name is required';
  if (!form.email.trim()) errors.email = 'Email is required';
  else if (!EMAIL_PATTERN.test(form.email.trim())) errors.email = 'Enter a valid email address';
  if (form.password.length < 8) errors.password = 'Password must be at least 8 characters';
  else if (form.password.length > 72) errors.password = 'Password must be at most 72 characters';
  if (form.confirmPassword !== undefined && form.confirmPassword !== form.password) {
    errors.confirmPassword = 'Passwords do not match';
  }
  return errors;
}

export default function SignupPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirmPassword: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to={homeFor(user)} replace />;

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    const newErrors = validateAccountForm(form);
    setErrors(newErrors);
    if (Object.keys(newErrors).length) return;

    setSubmitting(true);
    setError('');
    try {
      await api('/auth/signup', {
        method: 'POST',
        body: { fullName: form.fullName.trim(), email: form.email.trim(), password: form.password },
      });
      navigate('/login', { state: { registeredEmail: form.email.trim() } });
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="card auth-card">
        <div className="auth-card__brand">
          <img src="/favicon.svg" alt="" width="40" height="40" />
          <div>
            <h1>Create a student account</h1>
            <p className="muted">{APP_TITLE}</p>
            <p className="muted small">{INSTITUTION.programme}, {INSTITUTION.semester}</p>
          </div>
        </div>

        <Alert>{error}</Alert>

        <form onSubmit={onSubmit} noValidate>
          <Field label="Full name" htmlFor="fullName" error={errors.fullName} required>
            <input id="fullName" name="fullName" autoComplete="name" value={form.fullName} onChange={onChange} />
          </Field>
          <Field label="Email" htmlFor="email" error={errors.email} required>
            <input id="email" name="email" type="email" autoComplete="email" value={form.email} onChange={onChange} />
          </Field>
          <Field label="Password" htmlFor="password" error={errors.password} hint="At least 8 characters" required>
            <input id="password" name="password" type="password" autoComplete="new-password"
              value={form.password} onChange={onChange} />
          </Field>
          <Field label="Confirm password" htmlFor="confirmPassword" error={errors.confirmPassword} required>
            <input id="confirmPassword" name="confirmPassword" type="password" autoComplete="new-password"
              value={form.confirmPassword} onChange={onChange} />
          </Field>
          <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="muted small auth-card__note">
          OGE staff (International Lead) accounts can't be created here; an existing Lead creates them.
        </p>
        <p className="auth-card__footer">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </div>
    </div>
  );
}
