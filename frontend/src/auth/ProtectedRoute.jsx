import { Navigate, useLocation } from 'react-router-dom';
import { homeFor, useAuth } from './AuthContext';
import Spinner from '../components/Spinner';

/**
 * Wrap pages that need a login. With role="LEAD" or role="STUDENT" the page is
 * only shown to that role; others are sent to their own dashboard.
 * (The backend checks roles too — this is only for a nicer user experience.)
 */
export default function ProtectedRoute({ role, children }) {
  const { user, ready } = useAuth();
  const location = useLocation();

  if (!ready) return <Spinner label="Checking your session…" />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (role && user.role !== role) return <Navigate to={homeFor(user)} replace />;
  return children;
}
