import { useState } from 'react';
import { api } from '../../api/client';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import Badge, { SlotStatusBadge } from '../../components/Badge';
import ConfirmDialog from '../../components/ConfirmDialog';
import EmptyState from '../../components/EmptyState';
import Field from '../../components/Field';
import PageHeader from '../../components/PageHeader';
import Spinner from '../../components/Spinner';
import { errorText, formatDate, formatDateTime, formatTime, todayIso } from '../../utils/format';

const EMPTY_FORM = { date: '', startTime: '', endTime: '' };

function timeRange(x) {
  return `${formatTime(x.startTime)}–${formatTime(x.endTime)}`;
}

export default function SlotsPage() {
  const [showPast, setShowPast] = useState(false);
  const slots = useApi(`/appointments/slots?upcomingOnly=${!showPast}`);
  const bookings = useApi('/appointments/bookings');

  const [form, setForm] = useState(EMPTY_FORM);
  const [formErrors, setFormErrors] = useState({});
  const [creating, setCreating] = useState(false);
  const [pending, setPending] = useState(null); // { kind: 'slot' | 'booking', slot }
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const reloadAll = () => {
    slots.reload();
    bookings.reload();
  };

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const createSlot = async (e) => {
    e.preventDefault();
    const errs = {};
    if (!form.date) errs.date = 'Date is required';
    if (!form.startTime) errs.startTime = 'Start time is required';
    if (!form.endTime) errs.endTime = 'End time is required';
    if (form.startTime && form.endTime && form.endTime <= form.startTime) errs.endTime = 'End must be after start';
    setFormErrors(errs);
    if (Object.keys(errs).length) return;

    setCreating(true);
    setError('');
    setSuccess('');
    try {
      const slot = await api('/appointments/slots', { method: 'POST', body: form });
      setSuccess(`Slot created: ${formatDate(slot.date)}, ${timeRange(slot)}.`);
      setForm({ ...EMPTY_FORM, date: form.date }); // keep the date for adding the next slot quickly
      reloadAll();
    } catch (err) {
      setFormErrors(err.fieldErrors || {});
      setError(err.message);
    } finally {
      setCreating(false);
    }
  };

  const confirmPending = async () => {
    setWorking(true);
    setError('');
    try {
      if (pending.kind === 'slot') {
        await api(`/appointments/slots/${pending.slot.id}/cancel`, { method: 'POST' });
        setSuccess('Slot cancelled.');
      } else {
        await api(`/appointments/${pending.slot.appointmentId}/cancel`, { method: 'POST' });
        setSuccess(`Booking by ${pending.slot.studentName} cancelled. The slot is available again.`);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setWorking(false);
      setPending(null);
      reloadAll();
    }
  };

  return (
    <>
      <PageHeader title="Appointment slots" subtitle="Create times students can book, and see who has booked them." />
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>

      <form className="card form" onSubmit={createSlot} noValidate>
        <h2 className="card__title">New slot</h2>
        <div className="form__grid form__grid--3">
          <Field label="Date" htmlFor="date" error={formErrors.date} required>
            <input id="date" name="date" type="date" min={todayIso()} value={form.date} onChange={onChange} />
          </Field>
          <Field label="Start time" htmlFor="startTime" error={formErrors.startTime} required>
            <input id="startTime" name="startTime" type="time" value={form.startTime} onChange={onChange} />
          </Field>
          <Field label="End time" htmlFor="endTime" error={formErrors.endTime} required>
            <input id="endTime" name="endTime" type="time" value={form.endTime} onChange={onChange} />
          </Field>
        </div>
        <div className="form__actions">
          <button type="submit" className="btn btn--primary" disabled={creating}>
            {creating ? 'Creating…' : 'Create slot'}
          </button>
        </div>
      </form>

      <section className="card">
        <div className="card__header">
          <h2 className="card__title">My slots</h2>
          <label className="checkbox">
            <input type="checkbox" checked={showPast} onChange={(e) => setShowPast(e.target.checked)} />
            Show past slots
          </label>
        </div>
        {slots.loading && !slots.data && <Spinner inline />}
        {slots.error && <Alert>{errorText(slots.error)}</Alert>}
        {slots.data?.length === 0 && (
          <EmptyState title="No slots yet">Create a slot above so students can book a meeting.</EmptyState>
        )}
        {slots.data?.length > 0 && (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Time</th>
                  <th scope="col">Status</th>
                  <th scope="col">Booked by</th>
                  <th scope="col" className="table__actions-col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {slots.data.map((slot) => (
                  <tr key={slot.id} className={slot.past ? 'row--past' : undefined}>
                    <td>{formatDate(slot.date)}</td>
                    <td>{timeRange(slot)}</td>
                    <td><SlotStatusBadge status={slot.status} past={slot.past} /></td>
                    <td>
                      {slot.studentName ? (
                        <>
                          {slot.studentName}
                          <div className="muted small">{slot.studentEmail}</div>
                        </>
                      ) : <span className="muted">—</span>}
                    </td>
                    <td>
                      {!slot.past && slot.status !== 'CANCELLED' && (
                        <div className="row-actions">
                          {slot.status === 'BOOKED' && (
                            <button type="button" className="btn btn--small btn--ghost"
                              onClick={() => setPending({ kind: 'booking', slot })}>
                              Cancel booking
                            </button>
                          )}
                          <button type="button" className="btn btn--small btn--danger-ghost"
                            onClick={() => setPending({ kind: 'slot', slot })}>
                            Cancel slot
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="card">
        <h2 className="card__title">Booking overview</h2>
        {bookings.loading && !bookings.data && <Spinner inline />}
        {bookings.error && <Alert>{errorText(bookings.error)}</Alert>}
        {bookings.data?.length === 0 && <EmptyState title="No bookings yet." />}
        {bookings.data?.length > 0 && (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Time</th>
                  <th scope="col">Student</th>
                  <th scope="col">Status</th>
                  <th scope="col">Booked at</th>
                </tr>
              </thead>
              <tbody>
                {bookings.data.map((b) => (
                  <tr key={b.id}>
                    <td>{formatDate(b.date)}</td>
                    <td>{timeRange(b)}</td>
                    <td>
                      {b.studentName}
                      <div className="muted small">{b.studentEmail}</div>
                    </td>
                    <td>
                      {b.status === 'CANCELLED'
                        ? <Badge tone="bad">Cancelled by {b.cancelledBy === 'LEAD' ? 'you' : 'student'}</Badge>
                        : <Badge tone={b.upcoming ? 'info' : 'neutral'}>{b.upcoming ? 'Booked' : 'Completed'}</Badge>}
                    </td>
                    <td className="small">{formatDateTime(b.bookedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <ConfirmDialog
        open={pending != null}
        title={pending?.kind === 'slot' ? 'Cancel this slot?' : 'Cancel this booking?'}
        message={pending && (
          pending.kind === 'slot'
            ? `${formatDate(pending.slot.date)}, ${timeRange(pending.slot)}${pending.slot.studentName ? ` — the booking by ${pending.slot.studentName} will also be cancelled` : ''}. Students will no longer be able to book it.`
            : `${pending.slot.studentName}'s booking on ${formatDate(pending.slot.date)}, ${timeRange(pending.slot)} will be cancelled and the slot becomes available again.`
        )}
        confirmLabel={pending?.kind === 'slot' ? 'Cancel slot' : 'Cancel booking'}
        cancelLabel="Keep"
        danger
        busy={working}
        onConfirm={confirmPending}
        onCancel={() => setPending(null)}
      />
    </>
  );
}
