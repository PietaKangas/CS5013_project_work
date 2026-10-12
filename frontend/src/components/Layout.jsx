import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { APP_TITLE, INSTITUTION } from '../config/institution';

const STUDENT_LINKS = [
  { to: '/student', label: 'Dashboard', end: true },
  { to: '/student/profile', label: 'My profile' },
  { to: '/student/hostel', label: 'Hostel' },
  { to: '/student/documents', label: 'Documents' },
  { to: '/student/appointments', label: 'Appointments' },
];

const LEAD_LINKS = [
  { to: '/lead', label: 'Dashboard', end: true },
  { to: '/lead/students', label: 'Students' },
  { to: '/lead/appointments', label: 'Appointments' },
  { to: '/lead/settings', label: 'Settings' },
];

/** Page frame with the top navigation. Child pages render in <Outlet />. */
export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const links = user?.role === 'LEAD' ? LEAD_LINKS : STUDENT_LINKS;

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar__inner">
          <div className="brand">
            <img src="/favicon.svg" alt="" width="28" height="28" />
            <span title={APP_TITLE}>{INSTITUTION.officeShort} · {INSTITUTION.name}</span>
          </div>
          <nav className="nav" aria-label="Main">
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) => (isActive ? 'nav__link nav__link--active' : 'nav__link')}
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="topbar__user">
            <span className="topbar__name" title={user?.email}>
              {user?.fullName}
              <small>{user?.role === 'LEAD' ? 'International Lead, OGE' : 'International student'}</small>
            </span>
            <button type="button" className="btn btn--ghost btn--small" onClick={handleLogout}>
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  );
}
