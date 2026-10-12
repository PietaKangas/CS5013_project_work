import { useState } from 'react';
import { api } from '../../api/client';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import Badge from '../../components/Badge';
import Field from '../../components/Field';
import PageHeader from '../../components/PageHeader';
import Spinner from '../../components/Spinner';
import { CATEGORY_LABELS, errorText } from '../../utils/format';
import { validateAccountForm } from '../SignupPage';

const EMPTY_TYPE = { code: '', name: '', description: '', studentCategory: '', formUrl: '' };
const EMPTY_LEAD = { fullName: '', email: '', password: '' };

export default function SettingsPage() {
  return (
    <>
      <PageHeader title="Settings" subtitle="Required documents and Lead accounts." />
      <DocumentTypesSection />
      <CreateLeadSection />
    </>
  );
}

function DocumentTypesSection() {
  const types = useApi('/admin/document-types');
  const [form, setForm] = useState(EMPTY_TYPE);
  const [errors, setErrors] = useState({});
  const [busyId, setBusyId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const toggleActive = async (t) => {
    setBusyId(t.id);
    setError('');
    setSuccess('');
    try {
      await api(`/admin/document-types/${t.id}`, {
        method: 'PUT',
        body: {
          name: t.name, description: t.description, studentCategory: t.studentCategory,
          formUrl: t.formUrl, active: !t.active,
        },
      });
      setSuccess(`${t.name} is now ${t.active ? 'inactive (no longer required)' : 'active (required)'}.`);
      types.reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  };

  const create = async (e) => {
    e.preventDefault();
    const code = form.code.trim().toUpperCase().replace(/\s+/g, '_');
    const errs = {};
    if (!/^[A-Z0-9_]{2,50}$/.test(code)) errs.code = 'Use 2-50 letters, digits or _ (e.g. BANK_STATEMENT)';
    if (!form.name.trim()) errs.name = 'Name is required';
    setErrors(errs);
    if (Object.keys(errs).length) return;

    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const created = await api('/admin/document-types', {
        method: 'POST',
        body: { ...form, code, studentCategory: form.studentCategory || null, formUrl: form.formUrl.trim() || null },
      });
      setSuccess(`${created.name} added to the required documents.`);
      setForm(EMPTY_TYPE);
      types.reload();
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="card">
      <h2 className="card__title">Required document types</h2>
      <p className="muted small">
        Students see active types that apply to everyone or to their category. Deactivating a type
        removes it from every checklist but keeps files already uploaded.
      </p>
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>
      {types.loading && !types.data && <Spinner inline />}
      {types.error && <Alert>{errorText(types.error)}</Alert>}

      {types.data && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Document</th>
                <th scope="col">Required for</th>
                <th scope="col">Status</th>
                <th scope="col" className="table__actions-col"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {types.data.map((t) => (
                <tr key={t.id} className={t.active ? undefined : 'row--past'}>
                  <td>
                    <strong>{t.name}</strong> <code className="small">{t.code}</code>
                    {t.description && <div className="muted small">{t.description}</div>}
                    {t.formUrl && (
                      <a className="small form-link" href={t.formUrl} target="_blank" rel="noreferrer">Form link ↗</a>
                    )}
                  </td>
                  <td>{t.studentCategory ? CATEGORY_LABELS[t.studentCategory] + 's' : 'All students'}</td>
                  <td>{t.active ? <Badge tone="good">Active</Badge> : <Badge>Inactive</Badge>}</td>
                  <td>
                    <button type="button" className="btn btn--small btn--ghost" disabled={busyId === t.id}
                      onClick={() => toggleActive(t)}>
                      {t.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <form className="form form--nested" onSubmit={create} noValidate>
        <h3 className="form__section">Add a document type</h3>
        <div className="form__grid">
          <Field label="Name" htmlFor="type-name" error={errors.name} required>
            <input id="type-name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Field>
          <Field label="Code" htmlFor="type-code" error={errors.code} hint="Short unique id, e.g. BANK_STATEMENT" required>
            <input id="type-code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          </Field>
          <Field label="Required for" htmlFor="type-category">
            <select id="type-category" value={form.studentCategory}
              onChange={(e) => setForm({ ...form, studentCategory: e.target.value })}>
              <option value="">All students</option>
              {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}s only</option>
              ))}
            </select>
          </Field>
          <Field label="Description" htmlFor="type-description" error={errors.description}>
            <input id="type-description" value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </Field>
          <Field label="Form link (optional)" htmlFor="type-form-url" error={errors.formUrl}
            hint="A page where students download or fill in this form">
            <input id="type-form-url" type="url" placeholder="https://…" value={form.formUrl}
              onChange={(e) => setForm({ ...form, formUrl: e.target.value })} />
          </Field>
        </div>
        <div className="form__actions">
          <button type="submit" className="btn btn--primary" disabled={saving}>
            {saving ? 'Adding…' : 'Add document type'}
          </button>
        </div>
      </form>
    </section>
  );
}

function CreateLeadSection() {
  const [form, setForm] = useState(EMPTY_LEAD);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const errs = validateAccountForm(form);
    setErrors(errs);
    if (Object.keys(errs).length) return;

    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const lead = await api('/admin/leads', {
        method: 'POST',
        body: { fullName: form.fullName.trim(), email: form.email.trim(), password: form.password },
      });
      setSuccess(`Lead account created for ${lead.email}. Share the password with them securely.`);
      setForm(EMPTY_LEAD);
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="card">
      <h2 className="card__title">Add an International Lead</h2>
      <p className="muted small">Only existing Leads can create Lead accounts. Students sign up themselves.</p>
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <form className="form" onSubmit={submit} noValidate>
        <div className="form__grid form__grid--3">
          <Field label="Full name" htmlFor="lead-name" error={errors.fullName} required>
            <input id="lead-name" name="fullName" value={form.fullName} onChange={onChange} />
          </Field>
          <Field label="Email" htmlFor="lead-email" error={errors.email} required>
            <input id="lead-email" name="email" type="email" value={form.email} onChange={onChange} />
          </Field>
          <Field label="Temporary password" htmlFor="lead-password" error={errors.password} required>
            <input id="lead-password" name="password" type="password" autoComplete="new-password"
              value={form.password} onChange={onChange} />
          </Field>
        </div>
        <div className="form__actions">
          <button type="submit" className="btn btn--primary" disabled={saving}>
            {saving ? 'Creating…' : 'Create Lead account'}
          </button>
        </div>
      </form>
    </section>
  );
}
