// End-to-end API test for the Student Management System.
//
// Start the backend first (with LEAD_EMAIL / LEAD_PASSWORD set), then run:
//   node scripts/api-smoke-test.mjs
// Optional environment variables:
//   API_URL        default http://localhost:8080/api
//   LEAD_EMAIL     default lead@uni.edu
//   LEAD_PASSWORD  default LeadPass123
//
// It creates two new test students each run (unique emails), so it is safe to run repeatedly.
// Needs Node 18+ (built-in fetch, FormData and Blob).

const API = process.env.API_URL || 'http://localhost:8080/api';
const LEAD_EMAIL = process.env.LEAD_EMAIL || 'lead@uni.edu';
const LEAD_PASSWORD = process.env.LEAD_PASSWORD || 'LeadPass123';

let passed = 0;
let failed = 0;

function check(name, condition, detail = '') {
  if (condition) {
    passed++;
    console.log(`  PASS  ${name}`);
  } else {
    failed++;
    console.log(`  FAIL  ${name}${detail ? `  ->  ${detail}` : ''}`);
  }
}

async function call(method, path, { token, body, form } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) payload = form;
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const res = await fetch(API + path, { method, headers, body: payload });
  const type = res.headers.get('content-type') || '';
  let data = null;
  if (type.includes('application/json')) data = await res.json();
  else if (res.status !== 204) data = Buffer.from(await res.arrayBuffer());
  return { status: res.status, data };
}

const expectStatus = (name, res, status) =>
  check(`${name} (HTTP ${status})`, res.status === status, `got ${res.status} ${JSON.stringify(res.data?.message ?? '')}`);

function pdfFile(text) {
  const bytes = Buffer.from(`%PDF-1.4\n% ${text}\n%%EOF\n`, 'ascii');
  return new Blob([bytes], { type: 'application/pdf' });
}

function uploadForm(documentTypeId, blob, filename) {
  const form = new FormData();
  form.append('documentTypeId', String(documentTypeId));
  form.append('file', blob, filename);
  return form;
}

function pad(n) { return String(n).padStart(2, '0'); }
function isoDate(d) { return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; }

async function main() {
  console.log(`Testing ${API}\n`);
  const run = Date.now();
  const emailA = `alice.${run}@test.edu`;
  const emailB = `bob.${run}@test.edu`;
  const password = 'Student123';

  // ---------------- Module 1: authentication ----------------
  console.log('Authentication');
  expectStatus('Student A signs up', await call('POST', '/auth/signup', { body: { fullName: 'Alice Test', email: emailA, password } }), 201);
  expectStatus('Student B signs up', await call('POST', '/auth/signup', { body: { fullName: 'Bob Test', email: emailB, password } }), 201);
  expectStatus('Duplicate email is rejected', await call('POST', '/auth/signup', { body: { fullName: 'X', email: emailA, password } }), 409);
  const weak = await call('POST', '/auth/signup', { body: { fullName: 'X', email: `x.${run}@test.edu`, password: '123' } });
  expectStatus('Short password is rejected', weak, 400);
  check('  ...with a field error for "password"', Boolean(weak.data?.fieldErrors?.password));
  const sneaky = await call('POST', '/auth/signup', { body: { fullName: 'Sneaky', email: `sneaky.${run}@test.edu`, password, role: 'LEAD' } });
  check('Signing up with role=LEAD still creates a STUDENT', sneaky.data?.role === 'STUDENT', JSON.stringify(sneaky.data));
  expectStatus('Wrong password', await call('POST', '/auth/login', { body: { email: emailA, password: 'nope-nope' } }), 401);
  expectStatus('No token -> protected API refused', await call('GET', '/auth/me'), 401);
  expectStatus('Forged token -> refused', await call('GET', '/auth/me', { token: 'abc.def.ghi' }), 401);

  const loginA = await call('POST', '/auth/login', { body: { email: emailA, password } });
  const loginB = await call('POST', '/auth/login', { body: { email: emailB, password } });
  const loginL = await call('POST', '/auth/login', { body: { email: LEAD_EMAIL, password: LEAD_PASSWORD } });
  expectStatus('Student A logs in', loginA, 200);
  check('  ...role is STUDENT', loginA.data?.user?.role === 'STUDENT');
  expectStatus('Lead logs in (check LEAD_EMAIL/LEAD_PASSWORD if this fails)', loginL, 200);
  check('  ...role is LEAD', loginL.data?.user?.role === 'LEAD');
  if (loginA.status !== 200 || loginB.status !== 200 || loginL.status !== 200) {
    console.log('\nCannot continue without the three logins.');
    return;
  }
  const A = { token: loginA.data.token, id: loginA.data.user.id };
  const B = { token: loginB.data.token, id: loginB.data.user.id };
  const L = { token: loginL.data.token, id: loginL.data.user.id };
  expectStatus('Student cannot create a Lead', await call('POST', '/admin/leads', { token: A.token, body: { fullName: 'X', email: `x2.${run}@test.edu`, password } }), 403);

  // ---------------- Module 2: profiles ----------------
  console.log('\nProfiles and hostel');
  const profileBody = {
    fullName: 'Alice Test', phone: '+91 98765 43210', dateOfBirth: '2002-05-01', nationality: 'Finnish',
    address: '1 Campus Road', emergencyContact: 'Mum +358 40 123', studentNumber: `T${run % 1000000000}`,
    gender: 'FEMALE', passportNumber: 'T1234567', passportExpiry: '2031-01-01', arrivalDate: '2026-07-20',
    departureDate: '2026-11-28', homeUniversity: 'Test University',
    programme: 'MSc Computer Science', department: 'Computing', academicYear: 'Year 1', studentCategory: 'DEGREE',
  };
  const saved = await call('PUT', `/students/${A.id}/profile`, { token: A.token, body: profileBody });
  expectStatus('Student A saves a full profile', saved, 200);
  check('  ...profile is complete', saved.data?.profileComplete === true, JSON.stringify(saved.data?.missingFields));
  expectStatus('Student A cannot edit B\'s profile', await call('PUT', `/students/${B.id}/profile`, { token: A.token, body: profileBody }), 403);
  expectStatus('Student A cannot read B\'s profile', await call('GET', `/students/${B.id}/profile`, { token: A.token }), 403);
  expectStatus('Student A cannot list all students', await call('GET', '/students', { token: A.token }), 403);
  expectStatus('Same student ID for B is rejected', await call('PUT', `/students/${B.id}/profile`, { token: B.token, body: { ...profileBody, fullName: 'Bob Test' } }), 409);
  expectStatus('Invalid phone is rejected', await call('PUT', `/students/${A.id}/profile`, { token: A.token, body: { ...profileBody, phone: 'call me' } }), 400);
  expectStatus('Hostel ALLOCATED without room is rejected', await call('PUT', `/students/${A.id}/hostel`, { token: A.token, body: { hostelStatus: 'ALLOCATED' } }), 400);
  expectStatus('Hostel saved', await call('PUT', `/students/${A.id}/hostel`, { token: A.token, body: { hostelStatus: 'ALLOCATED', hostelName: 'Maple Hall', roomNumber: 'B-204' } }), 200);
  const leadView = await call('GET', `/students/${A.id}/profile`, { token: L.token });
  expectStatus('Lead reads A\'s profile with the Lead\'s own token', leadView, 200);
  check('  ...hostel data visible', leadView.data?.roomNumber === 'B-204');
  expectStatus('Lead finds A by student ID', await call('GET', `/students/by-student-number/${profileBody.studentNumber}`, { token: L.token }), 200);
  expectStatus('Lead cannot edit a student profile', await call('PUT', `/students/${A.id}/profile`, { token: L.token, body: profileBody }), 403);

  // ---------------- Module 3: documents ----------------
  console.log('\nDocuments');
  const checklist = await call('GET', '/documents/required', { token: A.token });
  expectStatus('Student A gets the checklist', checklist, 200);
  const items = checklist.data?.items ?? [];
  check('  ...degree-only documents are included for a DEGREE student', items.some((i) => i.code === 'DEGREE_CERTIFICATE'));
  check('  ...exchange-only documents are not', !items.some((i) => i.code === 'LEARNING_AGREEMENT'));
  const passport = items.find((i) => i.code === 'PASSPORT');

  const exe = new Blob([Buffer.from([0x4d, 0x5a, 0, 0, 1, 2, 3])], { type: 'application/pdf' });
  expectStatus('Renamed .exe disguised as PDF is rejected', await call('POST', '/documents', { token: A.token, form: uploadForm(passport.documentTypeId, exe, 'passport.pdf') }), 415);
  expectStatus('Lead cannot upload for a student', await call('POST', '/documents', { token: L.token, form: uploadForm(passport.documentTypeId, pdfFile('x'), 'p.pdf') }), 403);

  const up = await call('POST', '/documents', { token: A.token, form: uploadForm(passport.documentTypeId, pdfFile('first'), 'passport.pdf') });
  expectStatus('Student A uploads a PDF', up, 201);
  check('  ...response hides the server file path', up.data && !('storedFilename' in up.data) && !JSON.stringify(up.data).includes('uploads'));
  const docId = up.data?.id;

  expectStatus('Student B cannot see A\'s document', await call('GET', `/documents/${docId}`, { token: B.token }), 404);
  expectStatus('Student B cannot download A\'s file', await call('GET', `/documents/${docId}/file`, { token: B.token }), 404);
  expectStatus('Student B cannot delete A\'s document', await call('DELETE', `/documents/${docId}`, { token: B.token }), 403);
  const leadFile = await call('GET', `/documents/${docId}/file`, { token: L.token });
  expectStatus('Lead downloads A\'s file', leadFile, 200);
  check('  ...file content is intact', leadFile.data?.toString?.().includes('first'));

  const replaced = await call('POST', '/documents', { token: A.token, form: uploadForm(passport.documentTypeId, pdfFile('second'), 'passport-v2.pdf') });
  expectStatus('Uploading again replaces the document', replaced, 201);
  check('  ...same document row, new file name', replaced.data?.id === docId && replaced.data?.originalFilename === 'passport-v2.pdf');

  const leadChecklist = await call('GET', `/students/${A.id}/documents`, { token: L.token });
  expectStatus('Lead sees A\'s checklist', leadChecklist, 200);
  check('  ...completion calculated from uploads (1 uploaded)', leadChecklist.data?.uploadedCount === 1, JSON.stringify(leadChecklist.data?.uploadedCount));

  // ---------------- Review / verification workflow ----------------
  console.log('\nReview and verification');
  expectStatus('Student cannot review documents', await call('PUT', `/documents/${docId}/review`, { token: A.token, body: { decision: 'VERIFIED' } }), 403);
  expectStatus('Correction request needs a comment', await call('PUT', `/documents/${docId}/review`, { token: L.token, body: { decision: 'NEEDS_CORRECTION' } }), 400);
  expectStatus('Unknown decision is rejected', await call('PUT', `/documents/${docId}/review`, { token: L.token, body: { decision: 'MAYBE' } }), 400);
  expectStatus('Lead requests a correction', await call('PUT', `/documents/${docId}/review`, { token: L.token, body: { decision: 'NEEDS_CORRECTION', comment: 'Scan is blurry' } }), 200);
  const afterFix = await call('GET', '/documents/required', { token: A.token });
  const passportItem = afterFix.data?.items?.find((i) => i.code === 'PASSPORT');
  check('  ...student sees "needs correction" with the comment',
    passportItem?.document?.status === 'NEEDS_CORRECTION' && passportItem?.document?.reviewComment === 'Scan is blurry');
  const reup = await call('POST', '/documents', { token: A.token, form: uploadForm(passport.documentTypeId, pdfFile('third'), 'passport-v3.pdf') });
  check('Re-uploading resets it to "awaiting review"', reup.data?.status === 'UPLOADED' && !reup.data?.reviewComment, JSON.stringify(reup.data?.status));
  const verified = await call('PUT', `/documents/${docId}/review`, { token: L.token, body: { decision: 'VERIFIED' } });
  check('Lead verifies the document', verified.status === 200 && verified.data?.status === 'VERIFIED');
  const profileVerified = await call('PUT', `/students/${A.id}/profile/review`, { token: L.token, body: { decision: 'VERIFIED' } });
  check('Lead verifies the complete profile', profileVerified.data?.profileStatus === 'VERIFIED', JSON.stringify(profileVerified.data?.message));
  expectStatus('Incomplete profile cannot be verified', await call('PUT', `/students/${B.id}/profile/review`, { token: L.token, body: { decision: 'VERIFIED' } }), 400);
  expectStatus('Student cannot verify their own profile', await call('PUT', `/students/${A.id}/profile/review`, { token: A.token, body: { decision: 'VERIFIED' } }), 403);
  const resaved = await call('PUT', `/students/${A.id}/profile`, { token: A.token, body: profileBody });
  check('Saving unchanged details keeps it verified', resaved.data?.profileStatus === 'VERIFIED');
  const changed = await call('PUT', `/students/${A.id}/profile`, { token: A.token, body: { ...profileBody, phone: '+91 90000 11111' } });
  check('Changing verified details sends the profile back for review', changed.data?.profileStatus === 'PENDING');
  const leadHostel = await call('PUT', `/students/${A.id}/hostel`, { token: L.token, body: { hostelStatus: 'ALLOCATED', hostelName: 'Sabarmati Hostel', roomNumber: '216' } });
  check('Lead corrects the room number (counts as verified)', leadHostel.data?.roomNumber === '216' && leadHostel.data?.hostelReviewStatus === 'VERIFIED');
  expectStatus('Student cannot change another student\'s hostel', await call('PUT', `/students/${B.id}/hostel`, { token: A.token, body: { hostelStatus: 'APPLIED' } }), 403);
  const awaiting = await call('GET', `/students?reviewState=AWAITING_REVIEW&search=${run}`, { token: L.token });
  check('Filter "awaiting review" finds A (profile changed)', awaiting.data?.content?.some((s) => s.id === A.id));

  // ---------------- Module 4: Lead dashboard ----------------
  console.log('\nLead dashboard');
  const search = await call('GET', `/students?search=${encodeURIComponent(emailA)}`, { token: L.token });
  expectStatus('Search by email', search, 200);
  check('  ...finds student A', search.data?.content?.length === 1 && search.data.content[0].id === A.id);
  const missing = await call('GET', `/students?missingDocumentTypeId=${passport.documentTypeId}&size=100&search=${run}`, { token: L.token });
  check('Filter "missing passport" excludes A and includes B',
    missing.data && !missing.data.content.some((s) => s.id === A.id) && missing.data.content.some((s) => s.id === B.id));
  const sorted = await call('GET', `/students?search=${run}&sort=profile&direction=desc`, { token: L.token });
  check('Sort by profile completion (desc) puts A first', sorted.data?.content?.[0]?.id === A.id);
  const complete = await call('GET', `/students?profileComplete=true&search=${run}`, { token: L.token });
  check('Filter profileComplete=true returns only A', complete.data?.content?.length === 1 && complete.data.content[0].id === A.id);
  expectStatus('Stats endpoint', await call('GET', '/students/stats', { token: L.token }), 200);
  expectStatus('Bad filter value -> 400', await call('GET', '/students?hostelStatus=SOMETIMES', { token: L.token }), 400);

  expectStatus('Student A deletes the document', await call('DELETE', `/documents/${docId}`, { token: A.token }), 204);
  expectStatus('  ...and it is gone', await call('GET', `/documents/${docId}`, { token: A.token }), 404);

  // ---------------- Module 5: appointments ----------------
  console.log('\nAppointments');
  const day = new Date();
  day.setDate(day.getDate() + 30 + (run % 300));               // a random future day
  const date = isoDate(day);
  const hour = 8 + (run % 9);
  const slotBody = { date, startTime: `${pad(hour)}:00`, endTime: `${pad(hour)}:30` };

  expectStatus('Slot in the past is rejected', await call('POST', '/appointments/slots', { token: L.token, body: { date: '2020-01-01', startTime: '10:00', endTime: '10:30' } }), 400);
  expectStatus('End before start is rejected', await call('POST', '/appointments/slots', { token: L.token, body: { date, startTime: '11:00', endTime: '10:00' } }), 400);
  expectStatus('Student cannot create slots', await call('POST', '/appointments/slots', { token: A.token, body: slotBody }), 403);
  const slot = await call('POST', '/appointments/slots', { token: L.token, body: slotBody });
  expectStatus('Lead creates a slot', slot, 201);
  const slotId = slot.data?.id;
  expectStatus('Overlapping slot is rejected', await call('POST', '/appointments/slots', { token: L.token, body: { date, startTime: `${pad(hour)}:15`, endTime: `${pad(hour)}:45` } }), 409);

  const [bookA, bookB] = await Promise.all([
    call('POST', '/appointments', { token: A.token, body: { slotId } }),
    call('POST', '/appointments', { token: B.token, body: { slotId } }),
  ]);
  const statuses = [bookA.status, bookB.status].sort();
  check('Two students booking at the same moment: exactly one wins (201 + 409)', statuses[0] === 201 && statuses[1] === 409, `got ${statuses}`);
  const winner = bookA.status === 201 ? { ...A, appt: bookA.data } : { ...B, appt: bookB.data };
  const loser = bookA.status === 201 ? B : A;

  const mine = await call('GET', '/appointments/my', { token: winner.token });
  check('Winner sees the booking in "my appointments"', mine.data?.some((a) => a.id === winner.appt?.id && a.status === 'BOOKED'));
  const avail = await call('GET', '/appointments/available', { token: loser.token });
  check('Booked slot is no longer listed as available', !avail.data?.some((s) => s.id === slotId));
  expectStatus('Other student cannot cancel the winner\'s booking', await call('POST', `/appointments/${winner.appt?.id}/cancel`, { token: loser.token }), 404);
  expectStatus('Winner cancels own booking', await call('POST', `/appointments/${winner.appt?.id}/cancel`, { token: winner.token }), 200);
  const avail2 = await call('GET', '/appointments/available', { token: loser.token });
  check('Slot is available again after cancelling', avail2.data?.some((s) => s.id === slotId));
  const rebook = await call('POST', '/appointments', { token: loser.token, body: { slotId } });
  expectStatus('Other student can now book it', rebook, 201);
  const leadSlots = await call('GET', '/appointments/slots', { token: L.token });
  check('Lead sees who booked the slot', leadSlots.data?.find((s) => s.id === slotId)?.studentId === loser.id);
  expectStatus('Lead cancels the slot (and its booking)', await call('POST', `/appointments/slots/${slotId}/cancel`, { token: L.token }), 200);
  expectStatus('Cancelled slot cannot be booked', await call('POST', '/appointments', { token: winner.token, body: { slotId } }), 409);
  const history = await call('GET', '/appointments/my', { token: loser.token });
  check('Student history shows "cancelled by LEAD"', history.data?.some((a) => a.slotId === slotId && a.cancelledBy === 'LEAD'));
  expectStatus('Booking a non-existent slot', await call('POST', '/appointments', { token: A.token, body: { slotId: 999999999 } }), 404);

  // ---------------- Errors ----------------
  console.log('\nError handling');
  const broken = await fetch(`${API}/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{"email": ',
  });
  check('Malformed JSON -> 400', broken.status === 400, `got ${broken.status}`);
  expectStatus('Unknown student -> 404', await call('GET', '/students/999999999/profile', { token: L.token }), 404);
}

main()
  .catch((err) => {
    failed++;
    console.error('\nTest run crashed:', err.message);
    if (err.cause?.code === 'ECONNREFUSED') console.error('Is the backend running on', API, '?');
  })
  .finally(() => {
    console.log(`\n${passed} passed, ${failed} failed`);
    process.exitCode = failed ? 1 : 0;
  });
