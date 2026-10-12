import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { homeFor, useAuth } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import Layout from './components/Layout';
import Spinner from './components/Spinner';

import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';

import StudentDashboard from './pages/student/StudentDashboard';
import ProfilePage from './pages/student/ProfilePage';
import HostelPage from './pages/student/HostelPage';
import DocumentsPage from './pages/student/DocumentsPage';
import StudentAppointmentsPage from './pages/student/AppointmentsPage';

import LeadDashboard from './pages/lead/LeadDashboard';
import StudentListPage from './pages/lead/StudentListPage';
import StudentProfileView from './pages/lead/StudentProfileView';
import StudentDocumentsView from './pages/lead/StudentDocumentsView';
import SlotsPage from './pages/lead/SlotsPage';
import SettingsPage from './pages/lead/SettingsPage';

/** "/" sends people to the right place. */
function Home() {
  const { user, ready } = useAuth();
  if (!ready) return <Spinner />;
  return <Navigate to={user ? homeFor(user) : '/login'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      <Route path="/student" element={<ProtectedRoute role="STUDENT"><Layout /></ProtectedRoute>}>
        <Route index element={<StudentDashboard />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="hostel" element={<HostelPage />} />
        <Route path="documents" element={<DocumentsPage />} />
        <Route path="appointments" element={<StudentAppointmentsPage />} />
      </Route>

      <Route path="/lead" element={<ProtectedRoute role="LEAD"><Layout /></ProtectedRoute>}>
        <Route index element={<LeadDashboard />} />
        <Route path="students" element={<StudentListPage />} />
        <Route path="students/:id" element={<StudentProfileView />} />
        <Route path="students/:id/documents" element={<StudentDocumentsView />} />
        <Route path="appointments" element={<SlotsPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

function NotFound() {
  return (
    <div className="auth-page">
      <div className="card auth-card">
        <h1>Page not found</h1>
        <p className="muted">The page you are looking for doesn't exist.</p>
        <Link className="btn btn--primary" to="/">Go to my dashboard</Link>
      </div>
    </div>
  );
}
