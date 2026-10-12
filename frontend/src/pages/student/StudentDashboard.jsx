import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import { HostelBadge, ReviewBadge } from '../../components/Badge';
import PageHeader from '../../components/PageHeader';
import ProgressBar from '../../components/ProgressBar';
import Spinner from '../../components/Spinner';
import { errorText, formatDate, formatTime } from '../../utils/format';
import { INSTITUTION } from '../../config/institution';

/** Turns the profile + checklist into a to-do list for the student. */
function buildTodo(profile, checklist) {
  const todo = [];
  if (profile.profileStatus === 'NEEDS_CORRECTION') {
    todo.push({ tone: 'bad', text: 'Correct your profile', detail: profile.profileComment, to: '/student/profile' });
  }
  if (profile.hostelReviewStatus === 'NEEDS_CORRECTION') {
    todo.push({ tone: 'bad', text: 'Correct your hostel details', detail: profile.hostelComment, to: '/student/hostel' });
  }
  checklist.items
    .filter((i) => i.document?.status === 'NEEDS_CORRECTION')
    .forEach((i) => todo.push({
      tone: 'bad', text: `Upload a corrected ${i.name}`, detail: i.document.reviewComment, to: '/student/documents',
    }));
  if (!profile.profileComplete) {
    todo.push({ tone: 'warn', text: 'Complete your profile', detail: `Missing: ${profile.missingFields.join(', ')}`, to: '/student/profile' });
  }
  const missing = checklist.items.filter((i) => !i.document);
  if (missing.length) {
    todo.push({ tone: 'warn', text: `Upload ${missing.length} missing document${missing.length === 1 ? '' : 's'}`,
      detail: missing.map((i) => i.name).join(', '), to: '/student/documents' });
  }
  if (profile.hostelStatus !== 'ALLOCATED') {
    todo.push({ tone: 'warn', text: 'Add your hostel and room once allotted', to: '/student/hostel' });
  }
  return todo;
}

export default function StudentDashboard() {
  const { user } = useAuth();
  const profile = useApi('/students/me/profile');
  const checklist = useApi('/documents/required');
  const appointments = useApi('/appointments/my');

  const loading = profile.loading || checklist.loading || appointments.loading;
  const error = profile.error || checklist.error || appointments.error;
  const ready = !loading && !error && profile.data && checklist.data;

  const nextAppointment = appointments.data?.find((a) => a.status === 'BOOKED' && a.upcoming);
  const todo = ready ? buildTodo(profile.data, checklist.data) : [];

  return (
    <>
      <PageHeader title={`Welcome, ${user.fullName.split(' ')[0]}`} subtitle={`${INSTITUTION.programme}, ${INSTITUTION.semester}`} />
      {loading && <Spinner />}
      {error && <Alert>{errorText(error)}</Alert>}

      {ready && (
        <>
          <section className={todo.some((t) => t.tone === 'bad') ? 'card card--attention' : 'card'}>
            <h2 className="card__title">{todo.length ? 'What you still need to do' : 'You’re all set'}</h2>
            {todo.length === 0 ? (
              <p className="good-text">Everything is submitted. The International Office has nothing pending from you.</p>
            ) : (
              <ul className="todo">
                {todo.map((t) => (
                  <li key={t.text} className={`todo__item todo__item--${t.tone}`}>
                    <div>
                      <strong>{t.text}</strong>
                      {t.detail && <div className="small todo__detail">{t.detail}</div>}
                    </div>
                    <Link className="btn btn--small btn--ghost" to={t.to}>Open</Link>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <div className="grid grid--2">
            <section className="card">
              <h2 className="card__title">Profile</h2>
              <ProgressBar percent={profile.data.profileCompletionPercent} label="Profile completion" />
              <p>Review: <ReviewBadge status={profile.data.profileStatus} /></p>
              <Link className="btn btn--ghost btn--small" to="/student/profile">View / edit profile</Link>
            </section>

            <section className="card">
              <h2 className="card__title">Documents</h2>
              <ProgressBar
                percent={checklist.data.completionPercent}
                label={`${checklist.data.uploadedCount} of ${checklist.data.requiredCount} uploaded`}
              />
              <p className="muted small">
                {checklist.data.verifiedCount} verified
                {checklist.data.needsCorrectionCount > 0 && ` · ${checklist.data.needsCorrectionCount} need correction`}
              </p>
              <Link className="btn btn--ghost btn--small" to="/student/documents">Go to documents</Link>
            </section>

            <section className="card">
              <h2 className="card__title">Hostel</h2>
              <p><HostelBadge status={profile.data.hostelStatus} />{' '}
                {profile.data.hostelStatus !== 'NOT_APPLIED' && <ReviewBadge status={profile.data.hostelReviewStatus} />}
              </p>
              {profile.data.hostelStatus === 'ALLOCATED' ? (
                <p>{profile.data.hostelName}, room {profile.data.roomNumber}</p>
              ) : (
                <p className="muted">No room allocated yet.</p>
              )}
              <Link className="btn btn--ghost btn--small" to="/student/hostel">Hostel information</Link>
            </section>

            <section className="card">
              <h2 className="card__title">Next appointment</h2>
              {nextAppointment ? (
                <p>
                  <strong>{formatDate(nextAppointment.date)}</strong>, {formatTime(nextAppointment.startTime)}–
                  {formatTime(nextAppointment.endTime)} with {nextAppointment.leadName}
                </p>
              ) : (
                <p className="muted">You have no upcoming appointments.</p>
              )}
              <Link className="btn btn--ghost btn--small" to="/student/appointments">Book or manage</Link>
            </section>
          </div>
        </>
      )}
    </>
  );
}
