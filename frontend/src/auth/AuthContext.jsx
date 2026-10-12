import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, clearSession, loadSession, saveSession, setUnauthorizedHandler } from '../api/client';

const AuthContext = createContext(null);
const EXPIRED_KEY = 'sms.sessionExpired';

/** Where each role lands after logging in. */
export function homeFor(user) {
  return user?.role === 'LEAD' ? '/lead' : '/student';
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => loadSession());
  const [ready, setReady] = useState(false);

  const logout = useCallback(() => {
    clearSession();
    setSession(null);
  }, []);

  // Called by the API client when the server answers 401 to a logged-in request.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      sessionStorage.setItem(EXPIRED_KEY, '1');
      logout();
    });
  }, [logout]);

  // On page load, check that a stored token is still valid.
  useEffect(() => {
    if (!loadSession()?.token) {
      setReady(true);
      return;
    }
    api('/auth/me')
      .then((user) => {
        const updated = { ...loadSession(), user };
        saveSession(updated);
        setSession(updated);
      })
      .catch(() => logout())
      .finally(() => setReady(true));
  }, [logout]);

  const login = useCallback(async (email, password) => {
    const result = await api('/auth/login', { method: 'POST', body: { email, password } });
    const newSession = { token: result.token, user: result.user };
    saveSession(newSession);
    sessionStorage.removeItem(EXPIRED_KEY);
    setSession(newSession);
    return result.user;
  }, []);

  /** Keep the header in sync when the student changes their name. */
  const updateUser = useCallback((changes) => {
    setSession((current) => {
      if (!current) return current;
      const updated = { ...current, user: { ...current.user, ...changes } };
      saveSession(updated);
      return updated;
    });
  }, []);

  const value = useMemo(
    () => ({ user: session?.user ?? null, ready, login, logout, updateUser }),
    [session, ready, login, logout, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}

/** True once, right after an automatic logout because the session expired. */
export function consumeExpiredFlag() {
  const expired = sessionStorage.getItem(EXPIRED_KEY) === '1';
  sessionStorage.removeItem(EXPIRED_KEY);
  return expired;
}
