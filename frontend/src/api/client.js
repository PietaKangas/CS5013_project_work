// Small wrapper around fetch() used by every page.
// - adds "Authorization: Bearer <token>" when the user is logged in
// - turns error responses into an ApiError with the server's message
// - logs the user out automatically if the server says the token is no longer valid (401)

const BASE_URL = `${import.meta.env.VITE_API_BASE_URL || ''}/api`;
const SESSION_KEY = 'sms.session';

// ---------- stored session (token + user) ----------

export function loadSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY));
  } catch {
    return null;
  }
}

export function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

let unauthorizedHandler = () => {};

/** AuthContext registers its logout function here. */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

// ---------- errors ----------

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function toApiError(response) {
  let data = {};
  try {
    data = await response.json();
  } catch {
    // body was not JSON
  }
  return new ApiError(
    response.status,
    data.message || `Request failed (${response.status})`,
    data.fieldErrors || {},
  );
}

// ---------- requests ----------

async function send(path, { method = 'GET', body, formData, signal } = {}) {
  const session = loadSession();
  const headers = {};
  if (session?.token) headers.Authorization = `Bearer ${session.token}`;

  let payload;
  if (formData) {
    payload = formData; // the browser sets the multipart Content-Type itself
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(BASE_URL + path, { method, headers, body: payload, signal });
  } catch (err) {
    if (err.name === 'AbortError') throw err;
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?');
  }

  if (response.status === 401 && session?.token) {
    unauthorizedHandler(); // token expired or invalid -> log out
  }
  return response;
}

/** JSON request. Returns the parsed body (or null for 204 No Content). */
export async function api(path, options) {
  const response = await send(path, options);
  if (!response.ok) throw await toApiError(response);
  if (response.status === 204) return null;
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

/** Binary request (document files). Returns a Blob. */
export async function apiBlob(path) {
  const response = await send(path);
  if (!response.ok) throw await toApiError(response);
  return response.blob();
}
