import { useState } from 'react';
import { api } from '../../api/client';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import Badge from '../../components/Badge';
import ConfirmDialog from '../../components/ConfirmDialog';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import Spinner from '../../components/Spinner';
import { errorText, formatDate, formatTime } from '../../utils/format';

function timeRange(x) {
  return `${formatTime(x.startTime)}–${formatTime(x.endTime)}`;
}

function appointmentBadge(a) {
  if (a.status === 'CANCELLED') {
    return <Badge tone="bad">Cancelled{a.cancelledBy === 'LEAD' ? ' by office' : ''}</Badge>;
  }
  return a.upcoming ? <Badge tone="info">Booked</Badge> : <Badge tone="neutral">Completed</Badge>;
}

export default function AppointmentsPage() {
  const available = useApi('/appointments/available');
  const mine = useApi('/appointments/my');

  const [bookingId, setBookingId] = useState(null);
  const [toCancel, setToCancel] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const reloadAll = () => {
    available.reload();
    mine.reload();
  };

  const book = async (slot) => {
    setBookingId(slot.id);
    setError('');
    setSuccess('');
    try {
      await api('/appointments', { method: 'POST', body: { slotId: slot.id } });
      setSuccess(`Booked: ${formatDate(slot.date)}, ${timeRange(slot)} with ${slot.leadName}.`);
    } catch (err) {
      setError(err.message); // e.g. "This slot is already booked" if someone was faster
    } finally {
      setBookingId(null);
      reloadAll();
    }
  };

  const confirmCancel = async () => {
    setCancelling(true);
    setError('');
    try {
      await api(`/appointments/${toCancel.id}/cancel`, { method: 'POST' });
      setSuccess('Your appointment was cancelled.');
    } catch (err) {
      setError(err.message);
    } finally {
      setCancelling(false);
      setToCancel(null);
      reloadAll();
    }
  };

  const upcoming = mine.data?.filter((a) => a.status === 'BOOKED' && a.upcoming) ?? [];
  const history = mine.data?.filter((a) => !(a.status === 'BOOKED' && a.upcoming)) ?? [];

  return (
    <>
      <PageHeader title="Appointments" subtitle="Book a meeting with the International Office." />
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert onClose={() => setError('')}>{error}</Alert>

      <section className="card">
        <h2 className="card__title">My upcoming appointments</h2>
        {mine.loading && !mine.data && <Spinner inline />}
        {mine.error && <Alert>{errorText(mine.error)}</Alert>}
        {mine.data && upcoming.length === 0 && (
          <EmptyState title="No upcoming appointments">Pick a free time below to book one.</EmptyState>
        )}
        {upcoming.length > 0 && (
          <ul className="list">
            {upcoming.map((a) => (
              <li key={a.id} className="list__item">
                <div>
                  <strong>{formatDate(a.date)}</strong>, {timeRange(a)}
                  <div className="muted small">with {a.leadName}</div>
                </div>
                <button type="button" className="btn btn--small btn--danger-ghost" onClick={() => setToCancel(a)}>
                  Cancel
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <h2 className="card__title">Available times</h2>
        {available.loading && !available.data && <Spinner inline />}
        {available.error && <Alert>{errorText(available.error)}</Alert>}
        {available.data?.length === 0 && (
          <EmptyState title="No free times right now">Please check again later.</EmptyState>
        )}
        {available.data?.length > 0 && (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Time</th>
                  <th scope="col">With</th>
                  <th scope="col" className="table__actions-col"><span className="sr-only">Book</span></th>
                </tr>
              </thead>
              <tbody>
                {available.data.map((slot) => (
                  <tr key={slot.id}>
                    <td>{formatDate(slot.date)}</td>
                    <td>{timeRange(slot)}</td>
                    <td>{slot.leadName}</td>
                    <td>
                      <button type="button" className="btn btn--small btn--primary"
                        disabled={bookingId != null} onClick={() => book(slot)}>
                        {bookingId === slot.id ? 'Booking…' : 'Book'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {history.length > 0 && (
        <section className="card">
          <h2 className="card__title">History</h2>
          <ul className="list">
            {history.map((a) => (
              <li key={a.id} className="list__item">
                <div>
                  {formatDate(a.date)}, {timeRange(a)}
                  <div className="muted small">with {a.leadName}</div>
                </div>
                {appointmentBadge(a)}
              </li>
            ))}
          </ul>
        </section>
      )}

      <ConfirmDialog
        open={toCancel != null}
        title="Cancel this appointment?"
        message={toCancel && `${formatDate(toCancel.date)}, ${timeRange(toCancel)} with ${toCancel.leadName}. The time will become available to other students.`}
        confirmLabel="Cancel appointment"
        cancelLabel="Keep it"
        danger
        busy={cancelling}
        onConfirm={confirmCancel}
        onCancel={() => setToCancel(null)}
      />
    </>
  );
}
