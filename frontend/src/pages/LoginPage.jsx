import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { consumeExpiredFlag, homeFor, useAuth } from '../auth/AuthContext';
import Alert from '../components/Alert';
import Field from '../components/Field';
import { APP_TITLE, INSTITUTION } from '../config/institution';
import Spinner from '../components/Spinner';

export default function LoginPage() {
  const { user, ready, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ email: location.state?.registeredEmail || '', password: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [expired] = useState(() => consumeExpiredFlag());

  if (!ready) return <Spinner />;
  if (user) return <Navigate to={homeFor(user)} replace />;

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    const newErrors = {};
    if (!form.email.trim()) newErrors.email = 'Email is required';
    if (!form.password) newErrors.password = 'Password is required';
    setErrors(newErrors);
    if (Object.keys(newErrors).length) return;

    setSubmitting(true);
    setError('');
    try {
      const loggedIn = await login(form.email.trim(), form.password);
      // Go back to the page they wanted, if it belongs to their role; otherwise their dashboard.
      const from = location.state?.from;
      const home = homeFor(loggedIn);
      navigate(from && from.startsWith(home) ? from : home, { replace: true });
    } catch (err) {
      setError(err.status === 401 ? 'Invalid email or password.' : err.message);
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
            <h1>Log in</h1>
            <p className="muted">{APP_TITLE}</p>
            <p className="muted small">{INSTITUTION.programme}, {INSTITUTION.semester}</p>
          </div>
        </div>

        {expired && <Alert type="info">Your session has expired. Please log in again.</Alert>}
        {location.state?.registeredEmail && !error && (
          <Alert type="success">Account created. You can now log in.</Alert>
        )}
        <Alert>{error}</Alert>

        <form onSubmit={onSubmit} noValidate>
          <Field label="Email" htmlFor="email" error={errors.email}>
            <input id="email" name="email" type="email" autoComplete="email"
              value={form.email} onChange={onChange} />
          </Field>
          <Field label="Password" htmlFor="password" error={errors.password}>
            <input id="password" name="password" type="password" autoComplete="current-password"
              value={form.password} onChange={onChange} />
          </Field>
          <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
            {submitting ? 'Logging in…' : 'Log in'}
          </button>
        </form>

        <p className="auth-card__footer">
          New student? <Link to="/signup">Create an account</Link>
        </p>
      </div>
    </div>
  );
}
