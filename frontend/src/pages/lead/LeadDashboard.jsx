import { Link } from 'react-router-dom';
import { useApi } from '../../hooks/useApi';
import Alert from '../../components/Alert';
import { CompletionBadge } from '../../components/Badge';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import Spinner from '../../components/Spinner';
import { errorText, formatDate, formatTime } from '../../utils/format';

function Stat({ label, value, total, to, highlight = false }) {
  return (
    <Link className={highlight ? 'stat stat--highlight' : 'stat'} to={to}>
      <span className="stat__value">{value}{total != null && <small> / {total}</small>}</span>
      <span className="stat__label">{label}</span>
    </Link>
  );
}

export default function LeadDashboard() {
  const stats = useApi('/students/stats');
  const toReview = useApi('/students?reviewState=AWAITING_REVIEW&sort=name&size=6');
  const attention = useApi('/students?documentsComplete=false&sort=documents&direction=asc&size=6');
  const slots = useApi('/appointments/slots?upcomingOnly=true');

  const upcomingBookings = slots.data?.filter((s) => s.status === 'BOOKED') ?? [];
  const freeSlots = slots.data?.filter((s) => s.status === 'AVAILABLE').length ?? 0;

  return (
    <>
      <PageHeader
        title="Lead dashboard"
        subtitle="Overview of all registered international students."
        actions={<Link className="btn btn--primary" to="/lead/students">View all students</Link>}
      />

      {stats.loading && <Spinner />}
      {stats.error && <Alert>{errorText(stats.error)}</Alert>}
      {stats.data && (
        <div className="stats">
          <Stat label="Registered students" value={stats.data.totalStudents} to="/lead/students" />
          <Stat label="Awaiting your review" value={stats.data.awaitingReview}
            to="/lead/students?reviewState=AWAITING_REVIEW" highlight={stats.data.awaitingReview > 0} />
          <Stat label="Waiting on student corrections" value={stats.data.needsCorrection}
            to="/lead/students?reviewState=NEEDS_CORRECTION" />
          <Stat label="Fully verified" value={stats.data.fullyVerified} total={stats.data.totalStudents}
            to="/lead/students?reviewState=VERIFIED" />
          <Stat label="All documents submitted" value={stats.data.documentsComplete} total={stats.data.totalStudents}
            to="/lead/students?documentsComplete=false" />
          <Stat label="Hostel allocated" value={stats.data.hostelAllocated} total={stats.data.totalStudents}
            to="/lead/students?hostelStatus=ALLOCATED" />
        </div>
      )}

      <section className="card">
        <h2 className="card__title">Waiting for your review</h2>
        {toReview.loading && <Spinner inline />}
        {toReview.error && <Alert>{errorText(toReview.error)}</Alert>}
        {toReview.data?.content.length === 0 && <EmptyState title="Nothing to review right now." />}
        {toReview.data?.content.length > 0 && (
          <ul className="list">
            {toReview.data.content.map((s) => (
              <li key={s.id} className="list__item">
                <div>
                  <Link to={`/lead/students/${s.id}`}><strong>{s.fullName}</strong></Link>
                  <div className="muted small">
                    {[s.homeUniversity, s.documentsAwaitingReview > 0 && `${s.documentsAwaitingReview} document${s.documentsAwaitingReview === 1 ? '' : 's'} to check`,
                      s.profileComplete && s.profileStatus === 'PENDING' && 'profile', s.hostelStatus !== 'NOT_APPLIED' && s.hostelReviewStatus === 'PENDING' && 'hostel details']
                      .filter(Boolean).join(' · ')}
                  </div>
                </div>
                <Link className="btn btn--small btn--primary" to={`/lead/students/${s.id}`}>Review</Link>
              </li>
            ))}
          </ul>
        )}
        {toReview.data?.totalElements > 6 && (
          <Link className="btn btn--ghost btn--small" to="/lead/students?reviewState=AWAITING_REVIEW">
            See all {toReview.data.totalElements}
          </Link>
        )}
      </section>

      <div className="grid grid--2">
        <section className="card">
          <h2 className="card__title">Students with missing documents</h2>
          {attention.loading && <Spinner inline />}
          {attention.error && <Alert>{errorText(attention.error)}</Alert>}
          {attention.data?.content.length === 0 && (
            <EmptyState title="Everyone has submitted their documents." />
          )}
          {attention.data?.content.length > 0 && (
            <ul className="list">
              {attention.data.content.map((s) => (
                <li key={s.id} className="list__item">
                  <div>
                    <Link to={`/lead/students/${s.id}`}><strong>{s.fullName}</strong></Link>
                    <div className="muted small">Missing: {s.missingDocuments.join(', ')}</div>
                  </div>
                  <CompletionBadge complete={s.documentsComplete} percent={s.documentCompletionPercent} />
                </li>
              ))}
            </ul>
          )}
          <Link className="btn btn--ghost btn--small" to="/lead/students?documentsComplete=false">See all</Link>
        </section>

        <section className="card">
          <h2 className="card__title">Upcoming appointments</h2>
          {slots.loading && <Spinner inline />}
          {slots.error && <Alert>{errorText(slots.error)}</Alert>}
          {slots.data && (
            <p className="muted small">
              {upcomingBookings.length} booked · {freeSlots} free slot{freeSlots === 1 ? '' : 's'}
            </p>
          )}
          {slots.data && upcomingBookings.length === 0 && <EmptyState title="No upcoming bookings." />}
          {upcomingBookings.length > 0 && (
            <ul className="list">
              {upcomingBookings.slice(0, 6).map((s) => (
                <li key={s.id} className="list__item">
                  <div>
                    <strong>{formatDate(s.date)}</strong>, {formatTime(s.startTime)}–{formatTime(s.endTime)}
                    <div className="muted small">{s.studentName} · {s.studentEmail}</div>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <Link className="btn btn--ghost btn--small" to="/lead/appointments">Manage slots</Link>
        </section>
      </div>
    </>
  );
}
